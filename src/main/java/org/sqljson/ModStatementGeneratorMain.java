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
import com.fasterxml.jackson.module.jsonSchema.JsonSchema;
import com.fasterxml.jackson.module.jsonSchema.JsonSchemaGenerator;

import org.sqljson.dbmd.DatabaseMetadata;
import org.sqljson.specs.mod_stmts.ModSql;
import org.sqljson.util.AppUtils;
import org.sqljson.util.Optionals;
import org.sqljson.specs.mod_stmts.ModGroupSpec;


public class ModStatementGeneratorMain
{
   private static void printUsage()
   {
      PrintStream ps = System.out;
      ps.println("Expected arguments: [options] <db-metadata-file> <mods-spec-file> [<mods-output-dir>]");
      ps.println("If the output directory is not provided, then output is written to standard out.");
      ps.println("Options:");
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
         printModGroupSpecJsonSchema();
         return;
      }

      AppUtils.SplitArgs args = AppUtils.splitOptionsAndRequiredArgs(allArgs);

      if ( args.required.size() != 2 && args.required.size() != 3 )
         throw new RuntimeException("expected 2 or 3 non-option arguments");

      Path dbmdPath = Paths.get(args.required.get(0));
      if ( !Files.isRegularFile(dbmdPath) )
         AppUtils.throwError("Database metdata file not found.");

      Path modsSpecFilePath = Paths.get(args.required.get(1));
      if ( !Files.isRegularFile(modsSpecFilePath) )
         AppUtils.throwError("Mods specification file not found.");

      Optional<Path> outputDir = args.required.size() > 2 ? Optionals.opt(Paths.get(args.required.get(2))) : empty();

      try ( InputStream dbmdIS = Files.newInputStream(dbmdPath);
            InputStream modsSpecIS = Files.newInputStream(modsSpecFilePath) )
      {
         ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
         yamlMapper.registerModule(new Jdk8Module());

         DatabaseMetadata dbmd = yamlMapper.readValue(dbmdIS, DatabaseMetadata.class);
         ModGroupSpec modGroupSpec = yamlMapper.readValue(modsSpecIS, ModGroupSpec.class);

         outputDir.ifPresent(path ->  {
            if ( !Files.isDirectory(path) ) AppUtils.throwError("Mods output directory not found.");
         });

         ModStatementGenerator gen = new ModStatementGenerator(
            dbmd,
            modGroupSpec.getDefaultSchema(),
            new HashSet<>(modGroupSpec.getGenerateUnqualifiedNamesForSchemas())
         );

         List<ModSql> generatedMods = gen.generateModSqls(modGroupSpec.getModificationStatementSpecs());

         writeModSqls(generatedMods, outputDir);
      }
      catch(Exception e)
      {
         e.printStackTrace();
         System.err.println(e.getMessage());
         System.exit(1);
      }
   }

   private static void printModGroupSpecJsonSchema()
   {
      try
      {
         ObjectMapper objMapper = new ObjectMapper();
         objMapper.registerModule(new Jdk8Module());
         JsonSchemaGenerator schemaGen = new JsonSchemaGenerator(objMapper);
         JsonSchema schema = schemaGen.generateSchema(ModGroupSpec.class);
         objMapper.writeValue(System.out, schema);
      }
      catch(Exception e)
      {
         throw new RuntimeException(e);
      }
   }

   /**
    *
    * @param modSqls The modification SQL statements to be written.
    * @param outputDir The output directory in which to write directories if provided. If not provided, all queries
    *                  will be written to stdout.
    * @throws IOException if the output directory could not be created or a write operation fails
    * @return file paths written by mod name
    */
   private static Map<String,Path> writeModSqls
   (
      List<ModSql> modSqls,
      Optional<Path> outputDir
   )
      throws IOException
   {
      Map<String,Path> res = new HashMap<>();

      if ( outputDir.isPresent() )
         Files.createDirectories(outputDir.get());

      for ( ModSql mod: modSqls )
      {
         Optional<Path> outputFilePath = outputDir.map(d -> d.resolve(mod.getModName() + ".sql"));

         BufferedWriter bw = org.sqljson.util.Files.newFileOrStdoutWriter(outputFilePath);

         try
         {
            bw.write(
               "-- [ THIS STATEMENT WAS AUTO-GENERATED, ANY CHANGES MADE HERE MAY BE LOST. ]\n" +
               "-- " + mod.getModName() + "\n" +
               mod.getSql() + "\n"
            );

            outputFilePath.ifPresent(path -> res.put(mod.getModName(), path));
         }
         finally
         {
            if ( outputFilePath.isPresent() ) bw.close();
            else bw.flush();
         }
      }

      return res;
   }
}
