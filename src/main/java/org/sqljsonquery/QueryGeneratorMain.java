package org.sqljsonquery;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import static java.util.Optional.empty;
import static java.util.stream.Collectors.toList;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.module.jsonSchema.JsonSchema;
import com.fasterxml.jackson.module.jsonSchema.JsonSchemaGenerator;

import org.sqljsonquery.util.AppUtils.SplitArgs;
import org.sqljsonquery.util.Pair;
import static org.sqljsonquery.util.AppUtils.splitOptionsAndRequiredArgs;
import static org.sqljsonquery.util.AppUtils.throwError;
import static org.sqljsonquery.util.Files.readString;
import static org.sqljsonquery.util.Optionals.opt;
import static org.sqljsonquery.util.Files.newFileOrStdoutWriter;
import org.sqljsonquery.dbmd.DatabaseMetadata;
import org.sqljsonquery.query_spec.QueryGroupSpec;
import org.sqljsonquery.query_spec.ResultsRepr;
import org.sqljsonquery.types.SourceCodeWriter;
import org.sqljsonquery.types.source_writers.JavaWriter;
import org.sqljsonquery.types.source_writers.TypescriptWriter;
import static org.sqljsonquery.types.source_writers.JavaWriter.NullableFieldRepr;


public class QueryGeneratorMain
{
   private static final String langOptPrefix = "--types-language:";
   private static final String pkgOptPrefix = "--package:";
   private static final String javaNullabilityOptPrefix = "--java-nullability:";
   private static final String generatedTypesHeaderFileOptPrefix = "--types-file-header:";
   private static final String includeSourceGenerationTimestamp = "--include-source-gen-timestamp";

   private static void printUsage()
   {
      PrintStream ps = System.out;
      ps.println("Expected arguments: [options] <db-metadata-file> <queries-spec-file> " +
                 "[<src-output-base-dir> <queries-output-dir>]");
      ps.println("If output directories are not provided, then all output is written to standard out.");
      ps.println("Options:");
      ps.println("   " + langOptPrefix + "<language>  Output language, \"Java\"|\"Typescript\".");
      ps.println("   " + pkgOptPrefix + "<java-package>  The Java package for the generated query classes.");
      ps.println("   " + javaNullabilityOptPrefix + "<nullable-fields-option>  How nullable fields should be" +
                 "represented in Java.");
      ps.println("       Valid options are:");
      ps.println("         optwrapped : wrap the type with Optional<>");
      ps.println("         annotated  : annotate with JSR 305 @Nullable and @Nonnull.");
      ps.println("         baretype   : leave as bare type (Object variant for native types)");
      ps.println("   " + generatedTypesHeaderFileOptPrefix + "<file>  Contents of this file will be included at the " +
         "top of each generated type's source file (e.g. additional imports for overridden field types).");
      ps.println("    --print-query-group-spec-json-schema: Print a json schema for the query group spec, to " +
         "facilitate editing.");
   }

   public static void main(String[] allArgs)
   {
      if ( allArgs.length == 1 && allArgs[0].equals("-h") || allArgs[0].equals("--help") )
      {
         printUsage();
         return;
      }
      if ( allArgs.length == 1 && allArgs[0].equals("--print-query-group-spec-json-schema") )
      {
         printQueryGroupSpecJsonSchema();
         return;
      }

      SplitArgs args = splitOptionsAndRequiredArgs(allArgs);

      if ( args.required.size() != 2 && args.required.size() != 4 )
         throw new RuntimeException("expected 2 or 4 non-option arguments");

      Path dbmdPath = Paths.get(args.required.get(0));
      if ( !Files.isRegularFile(dbmdPath) )
         throwError("Database metdata file not found.");

      Path queriesSpecFilePath = Paths.get(args.required.get(1));
      if ( !Files.isRegularFile(queriesSpecFilePath) )
         throwError("Queries specification file not found.");

      Optional<Pair<Path,Path>> outputDirs = args.required.size() > 2 ?
         opt(Pair.make(Paths.get(args.required.get(2)), Paths.get(args.required.get(3))))
         : empty();

      try (InputStream dbmdIS = Files.newInputStream(dbmdPath);
           InputStream queriesSpecIS = Files.newInputStream(queriesSpecFilePath) )
      {
         ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
         yamlMapper.registerModule(new Jdk8Module());

         DatabaseMetadata dbmd = yamlMapper.readValue(dbmdIS, DatabaseMetadata.class);
         QueryGroupSpec queryGroupSpec = yamlMapper.readValue(queriesSpecIS, QueryGroupSpec.class);

         Optional<Path> srcOutputBaseDirPath = outputDirs.map(Pair::fst);
         srcOutputBaseDirPath.ifPresent(path ->  {
            if ( !Files.isDirectory(path) ) throwError("Source output base directory not found.");
         });

         Optional<Path> queriesOutputDirPath = outputDirs.map(Pair::snd);
         queriesOutputDirPath.ifPresent(path ->  {
            if ( !Files.isDirectory(path) ) throwError("Queries output directory not found.");
         });

         QueryGenerator gen = new QueryGenerator(
            dbmd,
            queryGroupSpec.getDefaultSchema(),
            new HashSet<>(queryGroupSpec.getGenerateUnqualifiedNamesForSchemas()),
            queryGroupSpec.getDefaultFieldOutputNameFunction()
         );

         List<SqlJsonQuery> generatedQueries = gen.generateSqlJsonQueries(queryGroupSpec.getQuerySpecs());

         List<WrittenQueryReprPath> writtenQueryPaths = writeQueries(generatedQueries, queriesOutputDirPath);

         List<SqlJsonQuery> queriesHavingTypes =
            generatedQueries.stream()
            .filter(gq -> !gq.getGeneratedResultTypes().isEmpty())
            .collect(toList());

         if ( !queriesHavingTypes.isEmpty() )
         {
            SourceCodeWriter srcWriter = getSourceCodeWriter(args, srcOutputBaseDirPath);
            boolean includeTimestamp = args.optional.contains(includeSourceGenerationTimestamp);
            srcWriter.writeSourceCode(queriesHavingTypes, writtenQueryPaths, includeTimestamp);
         }
      }
      catch(Exception e)
      {
         e.printStackTrace();
         System.err.println(e.getMessage());
         System.exit(1);
      }
   }

   private static void printQueryGroupSpecJsonSchema()
   {
      try
      {
         ObjectMapper objMapper = new ObjectMapper();
         objMapper.registerModule(new Jdk8Module());
         JsonSchemaGenerator schemaGen = new JsonSchemaGenerator(objMapper);
         JsonSchema schema = schemaGen.generateSchema(QueryGroupSpec.class);
         objMapper.writeValue(System.out, schema);
      }
      catch(Exception e)
      {
         throw new RuntimeException(e);
      }
   }

   private static SourceCodeWriter getSourceCodeWriter
   (
      SplitArgs args,
      Optional<Path> srcOutputBaseDir
   )
   {
      String language = "";
      String targetPackage = "";
      NullableFieldRepr nullableFieldRepr = NullableFieldRepr.ANNOTATED;
      Optional<String> typeFilesHeader = empty();
      for ( String opt : args.optional )
      {
         if ( opt.startsWith(langOptPrefix) ) language = opt.substring(langOptPrefix.length());
         else if ( opt.startsWith(pkgOptPrefix) ) targetPackage = opt.substring(pkgOptPrefix.length());
         else if ( opt.startsWith(javaNullabilityOptPrefix) ) nullableFieldRepr =
            NullableFieldRepr.valueOf(opt.substring(javaNullabilityOptPrefix.length()).toUpperCase());
         else if ( opt.startsWith(generatedTypesHeaderFileOptPrefix) ) typeFilesHeader =
            opt(readString(Paths.get(opt.substring(generatedTypesHeaderFileOptPrefix.length()))));
         else
            throw new RuntimeException("Unrecognized option \"" + opt + "\".");
      }

      switch ( language )
      {
         case "Java":
            return new JavaWriter(targetPackage, srcOutputBaseDir, nullableFieldRepr, typeFilesHeader);
         case "Typescript":
            return new TypescriptWriter(srcOutputBaseDir, typeFilesHeader);
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
      List<SqlJsonQuery> generatedQueries,
      Optional<Path> outputDir
   )
      throws IOException
   {
      List<WrittenQueryReprPath> res = new ArrayList<>();

      if ( outputDir.isPresent() )
         Files.createDirectories(outputDir.get());

      for ( SqlJsonQuery sjq : generatedQueries )
      {
         for ( ResultsRepr repr: sjq.getResultRepresentations() )
         {
            Optional<Path> outputFilePath = outputDir.map(d ->
               d.resolve(sjq.getQueryName() + "(" + repr.toString().toLowerCase().replace('_',' ') + ").sql")
            );

            BufferedWriter bw = newFileOrStdoutWriter(outputFilePath);

            try
            {
               bw.write(
                  "-- [ THIS QUERY WAS AUTO-GENERATED, ANY CHANGES MADE HERE MAY BE LOST. ]\n" +
                  "-- " + repr + " results representation for " + sjq.getQueryName() + "\n" +
                  sjq.getSql(repr) + "\n"
               );

               res.add(new WrittenQueryReprPath(sjq.getQueryName(), repr, outputFilePath));
            }
            finally
            {
               if ( outputFilePath.isPresent() ) bw.close();
               else bw.flush();
            }
         }
      }

      return res;
   }
}
