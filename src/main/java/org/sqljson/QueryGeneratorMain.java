package org.sqljson;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

import org.checkerframework.checker.nullness.qual.Nullable;

import org.sqljson.common.SourcesLanguage;
import org.sqljson.queries.GeneratedQuery;
import org.sqljson.queries.QueryGenerator;
import org.sqljson.queries.source_writers.SourceCodeWriter;
import org.sqljson.queries.source_writers.JavaWriter;
import org.sqljson.queries.source_writers.TypeScriptWriter;
import org.sqljson.queries.WrittenQueryReprPath;
import org.sqljson.queries.specs.QueryGroupSpec;
import org.sqljson.queries.specs.ResultsRepr;
import org.sqljson.common.specs.StatementSpecificationException;
import org.sqljson.util.AppUtils.SplitArgs;
import org.sqljson.dbmd.DatabaseMetadata;
import static org.sqljson.util.AppUtils.splitOptionsAndRequiredArgs;
import static org.sqljson.util.AppUtils.throwError;
import static org.sqljson.util.IO.newFileOrStdoutWriter;
import static org.sqljson.util.IO.readString;
import static org.sqljson.util.Nullables.ifPresent;
import static org.sqljson.util.Nullables.applyIfPresent;
import static org.sqljson.util.Serialization.getObjectMapper;
import static org.sqljson.util.Serialization.writeJsonSchema;


public class QueryGeneratorMain
{
   private static final String sqlResourcePathInGeneratedSourceOptPrefix = "--sql-resource-path-in-generated-src:";
   private static final String langOptPrefix = "--types-language:";
   private static final String pkgOptPrefix = "--package:";
   private static final String javaNullabilityOptPrefix = "--java-nullability:";
   private static final String generatedTypesHeaderFileOptPrefix = "--types-file-header:";
   private static final String includeSourceGenerationTimestamp = "--include-source-gen-timestamp";
   private static final String javaGenerateGetters = "--java-generate-getters";
   private static final String javaGenerateSetters = "--java-generate-setters";

   private static void printUsage()
   {
      PrintStream ps = System.out;
      ps.println("Expected arguments: [options] <db-metadata-file> <queries-spec-file> " +
                 "[<types-output-base-dir> <sql-output-dir>]");
      ps.println("If output directories are not provided, then all output is written to standard out.");
      ps.println("Options:");
      ps.println("   " + sqlResourcePathInGeneratedSourceOptPrefix + "<path>: a prefix to the SQL file name written into source code.");
      ps.println("   " + langOptPrefix + "<language>  Output language, \"Java\"|\"TypeScript\".");
      ps.println("   " + pkgOptPrefix + "<java-package>  The Java package for the generated query classes.");
      ps.println("   " + javaNullabilityOptPrefix + "<nullable-fields-option>  How nullable fields should be" +
                 "represented in Java.");
      ps.println("       Valid options are:");
      ps.println("         annotated  : annotate nullable fields with with @Nullable.");
      ps.println("         optwrapped : wrap the type with Optional<>");
      ps.println("         baretype   : leave as bare type (Object variant for native types)");
      ps.println("   " + javaGenerateGetters + "  Include getters in generated Java types.");
      ps.println("   " + javaGenerateSetters + "  Include setters in generated Java types.");
      ps.println("   " + generatedTypesHeaderFileOptPrefix + "<file>  Contents of this file will be included at the " +
         "top of each generated type's source file (e.g. additional imports for overridden field types).");
      ps.println("    --print-spec-json-schema: Print a json schema for the query group spec, to " +
         "facilitate editing.");
   }

   public static void main(String[] allArgs)
   {
      if ( allArgs.length == 1 && allArgs[0].equals("-h") || allArgs[0].equals("--help") )
      {
         printUsage();
         return;
      }
      if ( allArgs.length == 1 && allArgs[0].equals("--print-spec-json-schema") )
      {
         writeJsonSchema(QueryGroupSpec.class, System.out);
         return;
      }

      SplitArgs args = splitOptionsAndRequiredArgs(allArgs);

      if ( args.required.size() != 2 && args.required.size() != 4 )
         throw new RuntimeException("expected 2 or 4 non-option arguments");

      Path dbmdPath = Paths.get(args.required.get(0));
      if ( !Files.isRegularFile(dbmdPath) )
         throwError("Database metadata file not found.");

      Path queriesSpecFilePath = Paths.get(args.required.get(1));
      if ( !Files.isRegularFile(queriesSpecFilePath) )
         throwError("Queries specification file not found.");


      List<Path> outputDirs = args.required.size() > 2 ?
          Arrays.asList(Paths.get(args.required.get(2)), Paths.get(args.required.get(3)))
          : emptyList();

      try ( InputStream dbmdIS = Files.newInputStream(dbmdPath);
            InputStream queriesSpecIS = Files.newInputStream(queriesSpecFilePath) )
      {
         DatabaseMetadata dbmd = getObjectMapper(dbmdPath).readValue(dbmdIS, DatabaseMetadata.class);
         QueryGroupSpec queryGroupSpec = getObjectMapper(queriesSpecFilePath).readValue(queriesSpecIS, QueryGroupSpec.class);

         @Nullable Path srcOutputBaseDirPath = outputDirs.size() > 0 ? outputDirs.get(0) : null;
         ifPresent(srcOutputBaseDirPath, path ->  {
            if ( !Files.isDirectory(path) ) throwError("Source output base directory not found.");
         });

         @Nullable Path queriesOutputDirPath = outputDirs.size() > 1 ? outputDirs.get(1) : null;
         ifPresent(queriesOutputDirPath, path ->  {
            if ( !Files.isDirectory(path) ) throwError("Queries output directory not found.");
         });

         QueryGenerator gen =
            new QueryGenerator(
               dbmd,
               queryGroupSpec.getDefaultSchema(),
               new HashSet<>(queryGroupSpec.getGenerateUnqualifiedNamesForSchemas()),
               queryGroupSpec.getOutputFieldNameDefault().toFunctionOfFieldName(),
               queriesSpecFilePath.getFileName().toString()
            );

         List<GeneratedQuery> generatedQueries =
            queryGroupSpec.getQuerySpecs().stream()
            .map(gen::generateQuery)
            .collect(toList());

         List<WrittenQueryReprPath> writtenQueryPaths = writeQueries(generatedQueries, queriesOutputDirPath);

         List<GeneratedQuery> queriesWithSourceCodeEnabled =
            generatedQueries.stream()
            .filter(GeneratedQuery::getGenerateSourceEnabled)
            .collect(toList());

         if ( !queriesWithSourceCodeEnabled.isEmpty() )
         {
            SourceCodeWriter srcWriter = getSourceCodeWriter(args, srcOutputBaseDirPath);
            boolean includeTimestamp = args.optional.contains(includeSourceGenerationTimestamp);
            srcWriter.writeQueries(queriesWithSourceCodeEnabled, writtenQueryPaths, includeTimestamp);
         }
      }
      catch( StatementSpecificationException sse )
      {
         System.err.println();
         System.err.println();
         System.err.println("----------------------------------------------------------------------");
         System.err.println("Error in specification: " + sse.getStatementsSource());
         System.err.println("  in query: " + sse.getStatementName());
         System.err.println("  at part: " + sse.getStatementPart());
         System.err.println("  problem: " + sse.getProblem());
         System.err.println("----------------------------------------------------------------------");
         System.err.println();
         System.err.println();
      }
      catch(Exception e)
      {
         e.printStackTrace();
         System.err.println(e.getMessage());
         System.exit(1);
      }
   }

   private static SourceCodeWriter getSourceCodeWriter
      (
         SplitArgs args,
         @Nullable Path srcOutputBaseDir
      )
   {
      String langStr = "";
      String targetPackage = "";
      String sqlResourceNamePrefix = "";
      boolean generateJavaGetters = false;
      boolean generateJavaSetters = false;

      JavaWriter.NullableFieldRepr nullableFieldRepr = JavaWriter.NullableFieldRepr.ANNOTATED;
      @Nullable String typeFilesHeader = null;
      for ( String opt : args.optional )
      {
         if ( opt.startsWith(sqlResourcePathInGeneratedSourceOptPrefix) )
            sqlResourceNamePrefix = opt.substring(sqlResourcePathInGeneratedSourceOptPrefix.length());
         else if ( opt.startsWith(langOptPrefix) )
            langStr = opt.substring(langOptPrefix.length());
         else if ( opt.startsWith(pkgOptPrefix) )
            targetPackage = opt.substring(pkgOptPrefix.length());
         else if ( opt.startsWith(javaNullabilityOptPrefix) )
            nullableFieldRepr = JavaWriter.NullableFieldRepr.valueOf(opt.substring(javaNullabilityOptPrefix.length()).toUpperCase());
         else if ( opt.startsWith(generatedTypesHeaderFileOptPrefix) )
            typeFilesHeader = readString(Paths.get(opt.substring(generatedTypesHeaderFileOptPrefix.length())));
         else if ( opt.equals(javaGenerateGetters) )
            generateJavaGetters = true;
         else if ( opt.equals(javaGenerateSetters) )
            generateJavaSetters = true;
         else
            throw new RuntimeException("Unrecognized option \"" + opt + "\".");
      }

      SourcesLanguage sourcesLanguage = SourcesLanguage.valueOf(langStr);

      switch ( sourcesLanguage )
      {
         case Java:
            return new JavaWriter(
               targetPackage,
               srcOutputBaseDir,
               nullableFieldRepr,
               typeFilesHeader,
               sqlResourceNamePrefix,
               generateJavaGetters,
               generateJavaSetters
            );
         case TypeScript:
            return new TypeScriptWriter(
               srcOutputBaseDir,
               typeFilesHeader,
               sqlResourceNamePrefix
            );
         default:
            throw new RuntimeException("target language not supported");
      }
   }

   /**
    *
    * @param generatedQueries The queries to be written.
    * @param outputDir The output directory in which to write directories if provided. If not provided, all queries
    *                  will be written to stdout.
    * @throws IOException if the output directory could not be created or a write operation fails
    * @return A list of structures identifying the output locations of written queries.
    */
   private static List<WrittenQueryReprPath> writeQueries
      (
         List<GeneratedQuery> generatedQueries,
         @Nullable Path outputDir
      )
      throws IOException
   {
      List<WrittenQueryReprPath> res = new ArrayList<>();

      if ( outputDir != null )
         Files.createDirectories(outputDir);

      for ( GeneratedQuery q : generatedQueries )
      {
         for ( ResultsRepr repr: q.getResultRepresentations() )
         {
            String fileName = q.getQueryName() + "(" + repr.toString().toLowerCase().replace('_',' ') + ").sql";
            @Nullable Path outputFilePath = applyIfPresent(outputDir, d -> d.resolve(fileName));

            BufferedWriter bw = newFileOrStdoutWriter(outputFilePath);

            try
            {
               bw.write(
                  "-- [ THIS QUERY WAS AUTO-GENERATED, ANY CHANGES MADE HERE MAY BE LOST. ]\n" +
                  "-- " + repr + " results representation for " + q.getQueryName() + "\n" +
                  q.getSql(repr) + "\n"
               );

               res.add(new WrittenQueryReprPath(q.getQueryName(), repr, outputFilePath));
            }
            finally
            {
               if ( outputFilePath != null ) bw.close();
               else bw.flush();
            }
         }
      }

      return res;
   }
}

