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
import gov.fda.nctr.dbmd.DBMD;

import org.sqljsonquery.util.Pair;
import static org.sqljsonquery.util.Optionals.opt;
import static org.sqljsonquery.util.Files.newFileOrStdoutWriter;
import org.sqljsonquery.spec.QueriesSpec;
import org.sqljsonquery.spec.ResultsRepr;
import org.sqljsonquery.types.JavaWriter;
import org.sqljsonquery.types.SourceCodeWriter;


public class Main
{
   private static void printUsage(PrintStream ps)
   {
      ps.println("Expected arguments: [options] <db-metadata-file> <queries-spec-file> " +
                 "[<src-output-base-dir> <queries-output-dir>]");
      ps.println("If output directories are not provided, then all output is written to standard out.");
      ps.println("Options:");
      ps.println("   -p<java-package>");
      ps.println("   -l<language>: Output language, currently must be \"Java\".");
      ps.println("   -f<language>: Field declaration override file, having lines of the form: ");
      ps.println("        <queryName>.<generated-type-name>.<field-name>: <field type decl>");
   }

   public static void main(String[] allArgs)
   {
      if ( allArgs.length == 1 && allArgs[0].equals("-h") || allArgs[0].equals("--help") )
      {
         printUsage(System.out);
         System.exit(0);
      }

      SplitArgs args = splitOptionsAndRequiredArgs(allArgs);

      if ( args.required.size() != 2 && args.required.size() != 4 )
      {
         printUsage(System.err);
         System.exit(1);
      }

      Path dbmdPath = Paths.get(args.required.get(0));
      if ( !Files.isRegularFile(dbmdPath) )
         errorExit("Database metdata file not found.");

      Path queriesSpecFilePath = Paths.get(args.required.get(1));
      if ( !Files.isRegularFile(queriesSpecFilePath) )
         errorExit("Queries specification file not found.");

      Optional<Pair<Path,Path>> outputDirs = args.required.size() > 2 ?
         opt(Pair.make(Paths.get(args.required.get(2)), Paths.get(args.required.get(3))))
         : empty();

      try (InputStream dbmdIS = Files.newInputStream(dbmdPath);
           InputStream queriesSpecIS = Files.newInputStream(queriesSpecFilePath) )
      {
         ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
         yamlMapper.registerModule(new Jdk8Module());

         DBMD dbmd = yamlMapper.readValue(dbmdIS, DBMD.class);
         QueriesSpec queriesSpec = yamlMapper.readValue(queriesSpecIS, QueriesSpec.class);

         Optional<Path> srcOutputBaseDirPath = outputDirs.map(Pair::fst);
         srcOutputBaseDirPath.ifPresent(path ->  {
            if ( !Files.isDirectory(path) ) errorExit("Source output base directory not found.");
         });

         Optional<Path> queriesOutputDirPath = outputDirs.map(Pair::fst);
         queriesOutputDirPath.ifPresent(path ->  {
            if ( !Files.isDirectory(path) ) errorExit("Queries output directory not found.");
         });

         QueryGenerator gen = new QueryGenerator(dbmd, queriesSpec.getDefaultSchema());

         List<SqlJsonQuery> generatedQueries = gen.generateSqlJsonQueries(queriesSpec);

         writeQueries(generatedQueries, queriesOutputDirPath);

         SourceCodeWriter srcWriter = getSourceCodeWriter(args, srcOutputBaseDirPath);
         srcWriter.writeSourceCode(generatedQueries);
      }
      catch(Exception e)
      {
         e.printStackTrace();
         errorExit(e.getMessage());
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
      Optional<Map<String,String>> fieldTypeOverrides = Optional.empty();
      for ( String opt : args.optional )
      {
         if ( opt.startsWith("-p") ) targetPackage = opt.substring(2);
         if ( opt.startsWith("-l") ) language = opt.substring(2);
         if ( opt.startsWith("-f") ) fieldTypeOverrides = opt(readFieldTypeOverrides(Paths.get(opt.substring(2))));
      }

      switch ( language )
      {
         case "":
         case "Java":
            return new JavaWriter(targetPackage, srcOutputBaseDir, fieldTypeOverrides);
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
               d.resolve(sjq.getQueryName() + "[" + repr.toString() + "].sql")
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

   private static void errorExit(String message)
   {
      System.err.println(message);
      System.exit(-1);
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
