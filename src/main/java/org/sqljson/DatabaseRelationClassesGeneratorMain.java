package org.sqljson;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.checkerframework.checker.nullness.qual.Nullable;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;

import org.sqljson.dbmd.DatabaseMetadata;
import org.sqljson.source_writers.JavaWriter;
import org.sqljson.source_writers.SourceCodeWriter;
import org.sqljson.source_writers.TypescriptWriter;
import org.sqljson.specs.queries.ResultsRepr;
import org.sqljson.util.AppUtils.SplitArgs;
import static org.sqljson.util.AppUtils.splitOptionsAndRequiredArgs;
import static org.sqljson.util.AppUtils.throwError;
import static org.sqljson.util.IO.newFileOrStdoutWriter;
import static org.sqljson.util.Nullables.applyIfPresent;
import static org.sqljson.util.Nullables.ifPresent;


public class DatabaseRelationClassesGeneratorMain
{
   private static final String langOptPrefix = "--types-language:";
   private static final String pkgOptPrefix = "--package:";
   private static final String javaNullabilityOptPrefix = "--java-nullability:";
   private static final String includeSourceGenerationTimestamp = "--include-source-gen-timestamp";
   private static final String javaGenerateGetters = "--java-generate-getters";
   private static final String javaGenerateSetters = "--java-generate-setters";

   private static void printUsage()
   {
      PrintStream ps = System.out;
      ps.println("Expected arguments: [options] <db-metadata-file> [<src-output-base-dir>");
      ps.println("If output directory is not provided, then all output is written to standard out.");
      ps.println("Options:");
      ps.println("   " + langOptPrefix + "<language>  Output language, \"Java\"|\"Typescript\".");
      ps.println("   " + pkgOptPrefix + "<java-package>  The Java package for the generated source class(es).");
      ps.println("   " + javaNullabilityOptPrefix + "<nullable-fields-option>  How nullable fields should be" +
                 "represented in Java.");
      ps.println("       Valid options are:");
      ps.println("         annotated  : annotate nullable fields with with @Nullable.");
      ps.println("         optwrapped : wrap the type with Optional<>");
      ps.println("         baretype   : leave as bare type (Object variant for native types)");
      ps.println("   " + javaGenerateGetters + "  Include getters in generated Java types.");
      ps.println("   " + javaGenerateSetters + "  Include setters in generated Java types.");
   }

   public static void main(String[] allArgs)
   {
      if ( allArgs.length == 1 && allArgs[0].equals("-h") || allArgs[0].equals("--help") )
      {
         printUsage();
         return;
      }

      SplitArgs args = splitOptionsAndRequiredArgs(allArgs);

      if ( args.required.size() != 1 && args.required.size() != 2 )
         throw new RuntimeException("expected 1 or 2 non-option arguments");

      Path dbmdPath = Paths.get(args.required.get(0));
      if ( !Files.isRegularFile(dbmdPath) )
         throwError("Database metadata file not found.");

      @Nullable Path outputDir = args.required.size() > 1 ? Paths.get(args.required.get(1)) : null;

      try ( InputStream dbmdIS = Files.newInputStream(dbmdPath) )
      {
         ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
         yamlMapper.registerModule(new Jdk8Module());

         DatabaseMetadata dbmd = yamlMapper.readValue(dbmdIS, DatabaseMetadata.class);

         ifPresent(outputDir, path ->  {
            if ( !Files.isDirectory(path) ) throwError("Source output base directory not found.");
         });

         SourceCodeWriter srcWriter = getSourceCodeWriter(args, outputDir);
         boolean includeTimestamp = args.optional.contains(includeSourceGenerationTimestamp);
         srcWriter.writeRelationDefinitions(dbmd, includeTimestamp);
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
      boolean generateJavaGetters = false;
      boolean generateJavaSetters = false;

      JavaWriter.NullableFieldRepr nullableFieldRepr = JavaWriter.NullableFieldRepr.ANNOTATED;
      for ( String opt : args.optional )
      {
         if ( opt.startsWith(langOptPrefix) )
            langStr = opt.substring(langOptPrefix.length());
         else if ( opt.startsWith(pkgOptPrefix) )
            targetPackage = opt.substring(pkgOptPrefix.length());
         else if ( opt.startsWith(javaNullabilityOptPrefix) )
            nullableFieldRepr = JavaWriter.NullableFieldRepr.valueOf(opt.substring(javaNullabilityOptPrefix.length()).toUpperCase());
         else if ( opt.equals(javaGenerateGetters) )
            generateJavaGetters = true;
         else if ( opt.equals(javaGenerateSetters) )
            generateJavaSetters = true;
         else
            throw new RuntimeException("Unrecognized option \"" + opt + "\".");
      }

      TypesLanguage typesLanguage = TypesLanguage.valueOf(langStr);

      switch ( typesLanguage )
      {
         case Java:
            return new JavaWriter(
               targetPackage,
               srcOutputBaseDir,
               nullableFieldRepr,
               generateJavaGetters,
               generateJavaSetters
            );
         case Typescript:
            return new TypescriptWriter(srcOutputBaseDir, null, "");
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
