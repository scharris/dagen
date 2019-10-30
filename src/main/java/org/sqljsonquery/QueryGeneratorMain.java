package org.sqljsonquery;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import static java.util.Optional.empty;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;

import org.sqljsonquery.util.Pair;

import static org.sqljsonquery.util.Files.readString;
import static org.sqljsonquery.util.Optionals.opt;
import static org.sqljsonquery.util.Files.newFileOrStdoutWriter;
import org.sqljsonquery.dbmd.DatabaseMetadata;
import org.sqljsonquery.queryspec.QueryGroupSpec;
import org.sqljsonquery.queryspec.ResultsRepr;
import org.sqljsonquery.types.JavaWriter;
import org.sqljsonquery.types.SourceCodeWriter;
import static org.sqljsonquery.types.JavaWriter.NullableFieldRepr;


public class QueryGeneratorMain
{
   static final String langOptPrefix = "--types-language:";
   static final String pkgOptPrefix = "--package:";
   static final String javaNullabilityOptPrefix = "--java-nullability:";
   static final String fieldOverridesOptPrefix = "--field-overrides:";
   static final String generatedTypesHeaderFileOptPrefix = "--types-file-header:";

   private static void printUsage(PrintStream ps)
   {
      ps.println("Expected arguments: [options] <db-metadata-file> <queries-spec-file> " +
                 "[<src-output-base-dir> <queries-output-dir>]");
      ps.println("If output directories are not provided, then all output is written to standard out.");
      ps.println("Options:");
      ps.println("   " + langOptPrefix + "<language>  Output language, currently must be \"Java\".");
      ps.println("   " + pkgOptPrefix + "<java-package>  The Java package for the generated query classes.");
      ps.println("   " + javaNullabilityOptPrefix + "<nullable-fields-option>  How nullable fields should be" +
                 "represented in Java.");
      ps.println("       Valid options are:");
      ps.println("         optwrapped : wrap the type with Optional<>");
      ps.println("         annotated  : annotate with JSR 305 @Nullable and @Nonnull.");
      ps.println("         baretype   : leave as bare type (Object variant for native types)");
      ps.println("   " + fieldOverridesOptPrefix + "<field-type-overrides-file>  Field types override file, having" +
                 "lines of the form: ");
      ps.println("        <queryName>/<generated-type-name>.<field-name>: <field type decl>");
      ps.println("   " + generatedTypesHeaderFileOptPrefix + "<file>  Contents of this file will be included at the " +
         "top of each generated type's source file (e.g. additional imports for overridden field types).");
   }

   public static void main(String[] args)
   {
      try
      {
         execCommandLine(args);
      }
      catch(Exception e)
      {
         printUsage(System.err);
         System.exit(1);
      }
   }

   public static void execCommandLine(String[] allArgs)
   {
      if ( allArgs.length == 1 && allArgs[0].equals("-h") || allArgs[0].equals("--help") )
         printUsage(System.out);

      SplitArgs args = splitOptionsAndRequiredArgs(allArgs);

      if ( args.required.size() != 2 && args.required.size() != 4 )
         throw new RuntimeException("expected 2 or 4 non-option arguments");

      Path dbmdPath = Paths.get(args.required.get(0));
      if ( !Files.isRegularFile(dbmdPath) )
         error("Database metdata file not found.");

      Path queriesSpecFilePath = Paths.get(args.required.get(1));
      if ( !Files.isRegularFile(queriesSpecFilePath) )
         error("Queries specification file not found.");

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
            if ( !Files.isDirectory(path) ) error("Source output base directory not found.");
         });

         Optional<Path> queriesOutputDirPath = outputDirs.map(Pair::snd);
         queriesOutputDirPath.ifPresent(path ->  {
            if ( !Files.isDirectory(path) ) error("Queries output directory not found.");
         });

         QueryGenerator gen = new QueryGenerator(
            dbmd,
            queryGroupSpec.getDefaultSchema(),
            new HashSet<>(queryGroupSpec.getGenerateUnqualifiedNamesForSchemas()),
            queryGroupSpec.getDefaultOutputNameFunction()
         );

         List<SqlJsonQuery> generatedQueries = gen.generateSqlJsonQueries(queryGroupSpec.getQuerySpecs());

         writeQueries(generatedQueries, queriesOutputDirPath);

         SourceCodeWriter srcWriter = getSourceCodeWriter(args, srcOutputBaseDirPath);
         srcWriter.writeSourceCode(generatedQueries);
      }
      catch(Exception e)
      {
         error(e.getMessage());
      }
   }

   private static SplitArgs splitOptionsAndRequiredArgs(String[] args)
   {
      List<String> optArgs = new ArrayList<>();
      for (String arg : args)
         if (arg.startsWith("-")) optArgs.add(arg);
      return new SplitArgs(optArgs, Arrays.asList(Arrays.copyOfRange(args, optArgs.size(), args.length)));
   }

   private static SourceCodeWriter getSourceCodeWriter
   (
      SplitArgs args,
      Optional<Path> srcOutputBaseDir
   )
      throws IOException
   {
      String language = "";
      String targetPackage = "";
      NullableFieldRepr nullableFieldRepr = NullableFieldRepr.ANNOTATED;
      Optional<Map<String,String>> fieldTypeOverrides = empty();
      Optional<String> typeFilesHeader = empty();
      for ( String opt : args.optional )
      {
         if ( opt.startsWith(langOptPrefix) ) language = opt.substring(langOptPrefix.length());
         else if ( opt.startsWith(pkgOptPrefix) ) targetPackage = opt.substring(pkgOptPrefix.length());
         else if ( opt.startsWith(javaNullabilityOptPrefix) ) nullableFieldRepr =
            NullableFieldRepr.valueOf(opt.substring(javaNullabilityOptPrefix.length()).toUpperCase());
         else if ( opt.startsWith(fieldOverridesOptPrefix) ) fieldTypeOverrides =
            opt(readFieldTypeOverrides(Paths.get(opt.substring(fieldOverridesOptPrefix.length()))));
         else if ( opt.startsWith(generatedTypesHeaderFileOptPrefix) ) typeFilesHeader =
            opt(readString(Paths.get(opt.substring(generatedTypesHeaderFileOptPrefix.length()))));
         else
            throw new RuntimeException("Unrecognized option \"" + opt + "\".");
      }

      switch ( language )
      {
         case "":
         case "Java":
            return new JavaWriter(targetPackage, srcOutputBaseDir, fieldTypeOverrides, nullableFieldRepr, typeFilesHeader);
         default:
            throw new RuntimeException("target language not supported");
      }
   }

   private static Map<String,String> readFieldTypeOverrides(Path path) throws IOException
   {
      Map<String,String> res = new HashMap<>();

      for ( String line : Files.readAllLines(path) )
      {
         int firstColonIx = line.indexOf(':');
         if ( firstColonIx == -1 )
            throw new RuntimeException("invalid format in field type overrides file");
         String queryNameTypeNameFieldName = line.substring(0, firstColonIx).trim();
         String fieldType = line.substring(firstColonIx+1).trim();
         res.put(queryNameTypeNameFieldName, fieldType);
      }

      return res;
   }

   /**
    *
    * @param generatedQueries The queries to be written.
    * @param outputDir The output directory in which to write directories if provided. If not provided, all queries
    *                  will be written to stdout.
    * @throws IOException if the output directory could not be created or a write operation fails
    */
   private static void writeQueries
   (
      List<SqlJsonQuery> generatedQueries,
      Optional<Path> outputDir
   )
      throws IOException
   {
      if ( outputDir.isPresent() )
         Files.createDirectories(outputDir.get());

      for ( SqlJsonQuery sjq : generatedQueries )
      {
         for ( ResultsRepr repr: sjq.getResultRepresentations() )
         {
            Optional<Path> outputFilePath = outputDir.map(d ->
               d.resolve(sjq.getQueryName() + " [" + repr.toString() + "].sql")
            );

            BufferedWriter bw = newFileOrStdoutWriter(outputFilePath);

            try
            {
               bw.write(
                  "-- [ THIS QUERY WAS AUTO-GENERATED, ANY CHANGES MADE HERE MAY BE LOST. ]\n" +
                  "-- " + repr + " results representation for " + sjq.getQueryName() + "\n" +
                  sjq.getSql(repr) + "\n"
               );
            }
            finally
            {
               if ( outputFilePath.isPresent() ) bw.close();
               else bw.flush();
            }
         }
      }
   }

   private static void error(String message)
   {
      System.err.println(message);
      throw new RuntimeException(message);
   }

   private static class SplitArgs
   {
      public SplitArgs(List<String> optional, List<String> required)
      {
         this.optional = optional;
         this.required = required;
      }

      List<String> optional;
      List<String> required;
   }
}
