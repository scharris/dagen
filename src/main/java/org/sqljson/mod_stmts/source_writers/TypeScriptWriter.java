package org.sqljson.mod_stmts.source_writers;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;

import org.checkerframework.checker.nullness.qual.Nullable;

import org.sqljson.mod_stmts.GeneratedModStatement;
import static org.sqljson.util.IO.newFileOrStdoutWriter;
import static org.sqljson.util.Nullables.*;


public class TypeScriptWriter implements SourceCodeWriter
{
   private final @Nullable Path srcOutputDir;
   private final String sqlResourceNamePrefix;

   public TypeScriptWriter
      (
         @Nullable Path srcOutputDir,
         String sqlResourceNamePrefix
      )
   {
      this.srcOutputDir = srcOutputDir;
      this.sqlResourceNamePrefix = sqlResourceNamePrefix;
   }

   private void writeCommonSourceFileHeader
      (
         BufferedWriter bw,
         boolean includeTimestamp
      )
      throws IOException
   {
      bw.write("// ---------------------------------------------------------------------------\n");
      bw.write("// [ THIS SOURCE CODE WAS AUTO-GENERATED, ANY CHANGES MADE HERE MAY BE LOST. ]\n");
      if (includeTimestamp)
         bw.write("//   " + Instant.now().toString().replace('T', ' ') + "\n");
      bw.write("// ---------------------------------------------------------------------------\n");
   }

   @Override
   public void writeModStatements
      (
         List<GeneratedModStatement> generatedModStatements,
         Map<String,Path> writtenPathsByModName,
         boolean includeTimestamp
      )
      throws IOException
   {
      if ( srcOutputDir != null )
         Files.createDirectories(srcOutputDir);

      for ( GeneratedModStatement modStmt : generatedModStatements )
      {
         if ( !modStmt.getGenerateSource() ) continue;

         String moduleName = makeModuleName(modStmt.getStatementName());

         @Nullable Path outputFilePath = getOutputFilePath(moduleName);

         BufferedWriter bw = newFileOrStdoutWriter(outputFilePath);

         try
         {
            writeCommonSourceFileHeader(bw, includeTimestamp);

            bw.write("\n\n");

            writeModStatementModuleMembers(bw, modStmt, writtenPathsByModName);
         }
         finally
         {
            if ( outputFilePath != null ) bw.close();
            else bw.flush();
         }
      }
   }

   private void writeModStatementModuleMembers
      (
         BufferedWriter bw,
         GeneratedModStatement modStmt,
         Map<String,Path> sqlPathsByStatementName
      )
      throws IOException
   {
      // Write reference to SQL file if one was written.
      @Nullable Path writtenPath = sqlPathsByStatementName.get(modStmt.getStatementName());
      if ( writtenPath != null )
      {
         String resourceName = sqlResourceNamePrefix + writtenPath.getFileName().toString();
         bw.write("export const sqlResource = \"" + resourceName + "\";\n");
      }

      bw.write("\n");

      writeParamMembers(modStmt.getAllParameterNames(), bw, modStmt.hasNamedParameters());
   }

   private void writeParamMembers
      (
         List<String> paramNames,
         BufferedWriter bw,
         boolean hasNamedParams
      )
      throws IOException
   {
      for ( int paramIx = 0; paramIx < paramNames.size(); ++paramIx )
      {
         String paramName = paramNames.get(paramIx);

         bw.write("export const ");

         if ( hasNamedParams )
         {
            bw.write(paramName);
            bw.write("Param");
            bw.write(" = '");
            bw.write(paramName);
            bw.write("';\n\n");
         }
         else
         {
            bw.write(paramName);
            bw.write("ParamNum");
            bw.write(" = ");
            bw.write(String.valueOf(paramIx + 1));
            bw.write(";\n\n");
         }
      }
   }


   private static String makeModuleName(String statementName)
   {
      return statementName.replace(' ', '-').toLowerCase();
   }

   private @Nullable Path getOutputFilePath(String moduleName)
   {
      return applyIfPresent(srcOutputDir, d -> d.resolve(moduleName + ".ts"));
   }
}

