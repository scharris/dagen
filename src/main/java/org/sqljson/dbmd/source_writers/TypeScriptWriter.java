package org.sqljson.dbmd.source_writers;

import java.io.StringWriter;
import java.nio.file.Files;
import java.util.*;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import static java.util.stream.Collectors.groupingBy;

import org.checkerframework.checker.nullness.qual.Nullable;

import org.sqljson.dbmd.DatabaseMetadata;
import org.sqljson.dbmd.Field;
import org.sqljson.dbmd.RelId;
import org.sqljson.dbmd.RelMetadata;
import static org.sqljson.common.util.IO.newFileOrStdoutWriter;
import static org.sqljson.common.util.Nullables.*;
import static org.sqljson.common.util.StringFuns.indentLines;


public class TypeScriptWriter implements SourceCodeWriter
{
   private final @Nullable Path srcOutputDir;

   public TypeScriptWriter
      (
         @Nullable Path srcOutputDir
      )
   {
      this.srcOutputDir = srcOutputDir;
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
   public void writeRelationDefinitions
      (
         DatabaseMetadata dbmd,
         boolean includeTimestamp
      )
      throws IOException
   {
      if ( srcOutputDir != null )
         Files.createDirectories(srcOutputDir);

      @Nullable Path outputPath = getOutputFilePath();

      BufferedWriter bw = newFileOrStdoutWriter(outputPath);

      try
      {
         writeCommonSourceFileHeader(bw, includeTimestamp);

         writeRelationsModule(bw, dbmd);
      }
      finally
      {
         if ( outputPath != null ) bw.close();
         else bw.flush();
      }
   }

   private void writeRelationsModule
      (
         BufferedWriter bw,
         DatabaseMetadata dbmd
      )
      throws IOException
   {
      Map<String, List<RelMetadata>> relMdsBySchema =
         dbmd.getRelationMetadatas().stream()
         .collect(groupingBy(rmd -> valueOr(rmd.getRelationId().getSchema(), "DEFAULT")));

      for ( String schema : relMdsBySchema.keySet() )
      {
         bw.write("export const " + schema + " = {\n");

         for ( RelMetadata relMd : relMdsBySchema.get(schema) )
         {
            bw.write(indentLines(getRelationMetadataSource(relMd), 3));
            bw.write("\n");
         }

         bw.write("};\n"); // close schema object
      }
   }

   private String getRelationMetadataSource(RelMetadata relMd)
   {
      StringWriter sw = new StringWriter();

      RelId relId = relMd.getRelationId();

      sw.write("\"" + relId.getName() + "\": {\n");

      for ( Field f : relMd.getFields() )
      {
         sw.write("   " + asStringLiteral(f.getName()) + ": {");
         sw.write("\"type\": ");
         sw.write(asStringLiteral(f.getDatabaseType()) + ", ");
         sw.write("\"len\": ");
         sw.write(f.getLength() + ", ");
         sw.write("\"prec\":");
         sw.write(f.getPrecision() + ", ");
         sw.write("\"scale\": ");
         sw.write(f.getFractionalDigits() + ", ");
         sw.write("\"null\": ");
         sw.write(f.getNullable() + ", ");
         sw.write("\"pkPart\": ");
         @Nullable Integer pkPartNum = f.getPrimaryKeyPartNumber();
         sw.write(pkPartNum != null ? pkPartNum.toString() : "null");
         sw.write("},\n");
      }

      sw.write("},\n"); // close relation object

      return sw.toString();
   }

   private @Nullable Path getOutputFilePath()
   {
      return applyIfPresent(srcOutputDir, d -> d.resolve("relations.ts"));
   }

   private static String asStringLiteral(String s)
   {
      return "\"" + s.replace("\"", "\\\"") + "\"";
   }
}

