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
import static org.sqljson.util.StringFuns.*;


public class JavaWriter implements SourceCodeWriter
{
   private final String targetPackage;
   private final @Nullable Path packageOutputDir;
   private final String sqlResourceNamePrefix;

   public JavaWriter
      (
         String targetPackage,
         @Nullable Path srcOutputBaseDir,
         @Nullable String sqlResourceNamePrefix
      )
   {
      this.packageOutputDir = !targetPackage.isEmpty() ?
         applyIfPresent(srcOutputBaseDir, d -> d.resolve(targetPackage.replace('.','/')))
         : srcOutputBaseDir;
      this.targetPackage = targetPackage;
      this.sqlResourceNamePrefix = valueOr(sqlResourceNamePrefix, "");
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
      if ( packageOutputDir != null )
         Files.createDirectories(packageOutputDir);

      for ( GeneratedModStatement modStmt : generatedModStatements )
      {
         if ( !modStmt.getGenerateSource() ) continue;

         String className = upperCamelCase(modStmt.getStatementName());

         @Nullable Path outputFilePath = getOutputFilePath(className);

         BufferedWriter bw = newFileOrStdoutWriter(outputFilePath);

         try
         {
            writeCommonHeaderAndPackageDeclaration(bw, includeTimestamp);

            bw.write("\n\n");

            writeModStatementClass(bw, className, modStmt, writtenPathsByModName);
         }
         finally
         {
            if ( outputFilePath != null ) bw.close();
            else bw.flush();
         }
      }
   }

   private void writeModStatementClass
      (
         BufferedWriter bw,
         String className,
         GeneratedModStatement modStmt,
         Map<String,Path> sqlPathsByStatementName
      )
      throws IOException
   {
      bw.write("public class " + className + "\n");
      bw.write("{\n");

      // Write reference to SQL file if one was written.
      @Nullable Path writtenPath = sqlPathsByStatementName.get(modStmt.getStatementName());
      if ( writtenPath != null )
      {
         String resourceName = sqlResourceNamePrefix + writtenPath.getFileName().toString();
         bw.write("   public static final String sqlResource = \"" + resourceName + "\";\n");
      }

      bw.write("\n");

      writeParamMembers(modStmt.getAllParameterNames(), bw, modStmt.hasNamedParameters());

      bw.write("}\n");
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

         bw.write("   public static final ");

         if ( hasNamedParams )
         {
            bw.write("String ");
            bw.write(paramName);
            bw.write("Param");
            bw.write(" = \"");
            bw.write(paramName);
            bw.write("\";\n\n");
         }
         else
         {
            bw.write("int ");
            bw.write(paramName);
            bw.write("ParamNum");
            bw.write(" = ");
            bw.write(String.valueOf(paramIx + 1));
            bw.write(";\n\n");
         }
      }
   }

   private void writeCommonHeaderAndPackageDeclaration(BufferedWriter bw, boolean includeTimestamp) throws IOException
   {
      bw.write("// ---------------------------------------------------------------------------\n");
      bw.write("// [ THIS SOURCE CODE WAS AUTO-GENERATED, ANY CHANGES MADE HERE MAY BE LOST. ]\n");
      if (includeTimestamp)
         bw.write("//   " + Instant.now().toString().replace('T', ' ') + "\n");
      bw.write("// ---------------------------------------------------------------------------\n");
      if ( !targetPackage.isEmpty() )
         bw.write("package " + targetPackage + ";\n\n");
   }

   private @Nullable Path getOutputFilePath(String className)
   {
      return applyIfPresent(packageOutputDir, d -> d.resolve(className + ".java"));
   }
}
