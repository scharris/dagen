package org.sqljson;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import static java.util.Optional.empty;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;

import org.sqljson.util.*;
import org.sqljson.util.AppUtils.SplitArgs;
import static org.sqljson.util.AppUtils.splitOptionsAndRequiredArgs;
import static org.sqljson.util.AppUtils.throwError;
import static org.sqljson.util.Serialization.writeJsonSchema;
import org.sqljson.dbmd.DatabaseMetadata;
import org.sqljson.specs.mod_stmts.ModGroupSpec;
import org.sqljson.source_writers.*;


public class ModStatementGeneratorMain
{
   private static final String langOptPrefix = "--types-language:";
   private static final String pkgOptPrefix = "--package:";

   private static void printUsage()
   {
      PrintStream ps = System.out;
      ps.println("Expected arguments: [options] <db-metadata-file> <mods-spec-file> " +
                 "[<src-output-base-dir> <sql-output-dir>]");
      ps.println("If the output directory is not provided, then output is written to standard out.");
      ps.println("Options:");
      ps.println("   " + langOptPrefix + "<language>  Output language, \"Java\"|\"Typescript\".");
      ps.println("   " + pkgOptPrefix + "<java-package>  The Java package for the generated query classes.");
      ps.println("    --print-spec-json-schema: Print a json schema for the mod group spec, to facilitate editing.");
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
         writeJsonSchema(ModGroupSpec.class, System.out);
         return;
      }

      SplitArgs args = splitOptionsAndRequiredArgs(allArgs);

      if ( args.required.size() != 2 && args.required.size() != 4 )
         throw new RuntimeException("expected 2 or 4 non-option arguments");

      Path dbmdPath = Paths.get(args.required.get(0));
      if ( !Files.isRegularFile(dbmdPath) )
         AppUtils.throwError("Database metdata file not found.");

      Path modsSpecFilePath = Paths.get(args.required.get(1));
      if ( !Files.isRegularFile(modsSpecFilePath) )
         AppUtils.throwError("Mods specification file not found.");

      Optional<Pair<Path,Path>> outputDirs = args.required.size() > 2 ?
         Optionals.opt(Pair.make(Paths.get(args.required.get(2)), Paths.get(args.required.get(3))))
         : empty();

      try ( InputStream dbmdIS = Files.newInputStream(dbmdPath);
            InputStream modsSpecIS = Files.newInputStream(modsSpecFilePath) )
      {
         ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
         yamlMapper.registerModule(new Jdk8Module());

         DatabaseMetadata dbmd = yamlMapper.readValue(dbmdIS, DatabaseMetadata.class);
         ModGroupSpec modGroupSpec = yamlMapper.readValue(modsSpecIS, ModGroupSpec.class);

         Optional<Path> srcOutputBaseDirPath = outputDirs.map(Pair::fst);
         srcOutputBaseDirPath.ifPresent(path ->  {
            if ( !Files.isDirectory(path) ) throwError("Source output base directory not found.");
         });

         Optional<Path> modStmtsOutputDirPath = outputDirs.map(Pair::snd);
         modStmtsOutputDirPath.ifPresent(path ->  {
            if ( !Files.isDirectory(path) ) throwError("Mod statements output directory not found.");
         });

         ModStatementGenerator gen =
            new ModStatementGenerator(
               dbmd,
               modGroupSpec.getDefaultSchema(),
               new HashSet<>(modGroupSpec.getGenerateUnqualifiedNamesForSchemas())
            );

         List<GeneratedModStatement> generatedModStmts =
            gen.generateModStatements(modGroupSpec.getModificationStatementSpecs());

         Map<String,Path> writtenPathsByModName = writeModSqls(generatedModStmts, modStmtsOutputDirPath);

         if ( generatedModStmts.stream().anyMatch(GeneratedModStatement::getGenerateSource) )
         {
            SourceCodeWriter srcWriter = getSourceCodeWriter(args, srcOutputBaseDirPath);
            srcWriter.writeModStatements(generatedModStmts, writtenPathsByModName, false);
         }
      }
      catch(Exception e)
      {
         e.printStackTrace();
         System.err.println(e.getMessage());
         System.exit(1);
      }
   }

   /**
    *
    * @param generatedModStatements The modification SQL statements to be written.
    * @param outputDir The output directory in which to write directories if provided. If not provided, all queries
    *                  will be written to stdout.
    * @throws IOException if the output directory could not be created or a write operation fails
    * @return file paths written by mod name
    */
   private static Map<String,Path> writeModSqls
   (
      List<GeneratedModStatement> generatedModStatements,
      Optional<Path> outputDir
   )
      throws IOException
   {
      Map<String,Path> res = new HashMap<>();

      if ( outputDir.isPresent() )
         Files.createDirectories(outputDir.get());

      for ( GeneratedModStatement mod: generatedModStatements)
      {
         Optional<Path> outputFilePath = outputDir.map(d -> d.resolve(mod.getStatementName() + ".sql"));

         BufferedWriter bw = org.sqljson.util.Files.newFileOrStdoutWriter(outputFilePath);

         try
         {
            bw.write(
               "-- [ THIS STATEMENT WAS AUTO-GENERATED, ANY CHANGES MADE HERE MAY BE LOST. ]\n" +
               "-- " + mod.getStatementName() + "\n" +
               mod.getSql() + "\n"
            );

            outputFilePath.ifPresent(path -> res.put(mod.getStatementName(), path));
         }
         finally
         {
            if ( outputFilePath.isPresent() ) bw.close();
            else bw.flush();
         }
      }

      return res;
   }

   private static SourceCodeWriter getSourceCodeWriter
   (
      SplitArgs args,
      Optional<Path> srcOutputBaseDir
   )
   {
      String language = "";
      String targetPackage = "";
      for ( String opt : args.optional )
      {
         if ( opt.startsWith(langOptPrefix) ) language = opt.substring(langOptPrefix.length());
         else if ( opt.startsWith(pkgOptPrefix) ) targetPackage = opt.substring(pkgOptPrefix.length());
         else
            throw new RuntimeException("Unrecognized option \"" + opt + "\".");
      }

      switch ( language )
      {
         case "Java":
            return new JavaWriter(targetPackage, srcOutputBaseDir);
         case "Typescript":
            return new TypescriptWriter(srcOutputBaseDir, empty());
         default:
            throw new RuntimeException("target language not supported");
      }
   }
}
