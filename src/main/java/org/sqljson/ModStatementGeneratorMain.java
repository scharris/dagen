package org.sqljson;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import static java.util.stream.Collectors.toList;
import static java.util.Collections.emptyList;

import org.checkerframework.checker.nullness.qual.Nullable;

import org.sqljson.mod_stmts.GeneratedModStatement;
import org.sqljson.mod_stmts.ModStatementGenerator;
import org.sqljson.mod_stmts.source_writers.JavaWriter;
import org.sqljson.mod_stmts.source_writers.SourceCodeWriter;
import org.sqljson.mod_stmts.source_writers.TypeScriptWriter;
import org.sqljson.util.*;
import org.sqljson.util.AppUtils.SplitArgs;
import org.sqljson.dbmd.DatabaseMetadata;
import org.sqljson.mod_stmts.specs.ModGroupSpec;
import static org.sqljson.util.AppUtils.splitOptionsAndRequiredArgs;
import static org.sqljson.util.AppUtils.throwError;
import static org.sqljson.util.IO.newFileOrStdoutWriter;
import static org.sqljson.util.Nullables.ifPresent;
import static org.sqljson.util.Nullables.applyIfPresent;
import static org.sqljson.util.Serialization.getObjectMapper;
import static org.sqljson.util.Serialization.writeJsonSchema;


public class ModStatementGeneratorMain
{
   private static final String sqlResourcePathInGeneratedSourceOptPrefix = "--sql-resource-path-in-generated-src:";
   private static final String langOptPrefix = "--types-language:";
   private static final String pkgOptPrefix = "--package:";

   private static void printUsage()
   {
      PrintStream ps = System.out;
      ps.println("Expected arguments: [options] <db-metadata-file> <mods-spec-file> " +
                 "[<src-output-base-dir> <sql-output-dir>]");
      ps.println("If the output directory is not provided, then output is written to standard out.");
      ps.println("Options:");
      ps.println("   " + sqlResourcePathInGeneratedSourceOptPrefix + "<path>: a prefix to the SQL file name written into source code.");
      ps.println("   " + langOptPrefix + "<language>  Output language, \"Java\"|\"TypeScript\".");
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
         AppUtils.throwError("Database metadata file not found.");

      Path modsSpecFilePath = Paths.get(args.required.get(1));
      if ( !Files.isRegularFile(modsSpecFilePath) )
         AppUtils.throwError("Mods specification file not found.");

      List<Path> outputDirs = args.required.size() > 2 ?
         Arrays.asList(Paths.get(args.required.get(2)), Paths.get(args.required.get(3)))
         : emptyList();

      try ( InputStream dbmdIS = Files.newInputStream(dbmdPath);
            InputStream modsSpecIS = Files.newInputStream(modsSpecFilePath) )
      {
         DatabaseMetadata dbmd = getObjectMapper(dbmdPath).readValue(dbmdIS, DatabaseMetadata.class);
         ModGroupSpec modGroupSpec  = getObjectMapper(modsSpecFilePath).readValue(modsSpecIS, ModGroupSpec.class);

         @Nullable Path srcOutputBaseDirPath = outputDirs.size() > 0 ? outputDirs.get(0) : null;
         ifPresent(srcOutputBaseDirPath, path ->  {
            if ( !Files.isDirectory(path) ) throwError("Source output base directory not found.");
         });

         @Nullable Path modStmtsOutputDirPath = outputDirs.size() > 1 ? outputDirs.get(1) : null;
         ifPresent(modStmtsOutputDirPath, path ->  {
            if ( !Files.isDirectory(path) ) throwError("Mod statements output directory not found.");
         });

         ModStatementGenerator gen =
            new ModStatementGenerator(
               dbmd,
               modGroupSpec.getDefaultSchema(),
               new HashSet<>(modGroupSpec.getGenerateUnqualifiedNamesForSchemas()),
               modsSpecFilePath.getFileName().toString()
            );

         List<GeneratedModStatement> generatedModStmts =
            modGroupSpec.getModificationStatementSpecs().stream()
            .map(gen::generateModStatement)
            .collect(toList());

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
         @Nullable Path outputDir
      )
      throws IOException
   {
      Map<String,Path> res = new HashMap<>();

      if ( outputDir != null )
         Files.createDirectories(outputDir);

      for ( GeneratedModStatement mod: generatedModStatements)
      {
         @Nullable Path outputFilePath = applyIfPresent(outputDir, d -> d.resolve(mod.getStatementName() + ".sql"));

         BufferedWriter bw = newFileOrStdoutWriter(outputFilePath);

         try
         {
            bw.write(
               "-- [ THIS STATEMENT WAS AUTO-GENERATED, ANY CHANGES MADE HERE MAY BE LOST. ]\n" +
               "-- " + mod.getStatementName() + "\n" +
               mod.getSql() + "\n"
            );

            ifPresent(outputFilePath, path -> res.put(mod.getStatementName(), path));
         }
         finally
         {
            if ( outputFilePath != null ) bw.close();
            else bw.flush();
         }
      }

      return res;
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
      for ( String opt : args.optional )
      {
         if ( opt.startsWith(sqlResourcePathInGeneratedSourceOptPrefix) )
            sqlResourceNamePrefix = opt.substring(sqlResourcePathInGeneratedSourceOptPrefix.length());
         else if ( opt.startsWith(langOptPrefix) )
            langStr = opt.substring(langOptPrefix.length());
         else if ( opt.startsWith(pkgOptPrefix) ) targetPackage = opt.substring(pkgOptPrefix.length());
         else
            throw new RuntimeException("Unrecognized option \"" + opt + "\".");
      }

      SourcesLanguage sourcesLanguage = SourcesLanguage.valueOf(langStr);

      switch (sourcesLanguage)
      {
         case Java:
            return new JavaWriter(targetPackage, srcOutputBaseDir, sqlResourceNamePrefix);
         case TypeScript:
            return new TypeScriptWriter(srcOutputBaseDir, sqlResourceNamePrefix);
         default:
            throw new RuntimeException("target language not supported");
      }
   }
}

