package org.sqljson.source_writers;

import java.io.StringWriter;
import java.nio.file.Files;
import java.util.*;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.sql.Types;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

import org.checkerframework.checker.nullness.qual.Nullable;

import org.sqljson.GeneratedModStatement;
import org.sqljson.GeneratedQuery;
import org.sqljson.dbmd.DatabaseMetadata;
import org.sqljson.dbmd.Field;
import org.sqljson.dbmd.RelId;
import org.sqljson.dbmd.RelMetadata;
import org.sqljson.result_types.*;
import org.sqljson.specs.queries.ResultsRepr;
import org.sqljson.util.IO;
import org.sqljson.WrittenQueryReprPath;
import static org.sqljson.WrittenQueryReprPath.getWrittenSqlPathsForQuery;
import static org.sqljson.util.IO.newFileOrStdoutWriter;
import static org.sqljson.util.IO.writeString;
import static org.sqljson.util.Nullables.*;
import static org.sqljson.util.StringFuns.indentLines;
import static org.sqljson.util.StringFuns.upperCamelCase;


public class TypeScriptWriter implements SourceCodeWriter
{
   private final @Nullable Path srcOutputDir;
   private final @Nullable String filesHeader;
   private final String sqlResourceNamePrefix;

   public TypeScriptWriter
      (
         @Nullable Path srcOutputDir,
         @Nullable String filesHeader,
         String sqlResourceNamePrefix
      )
   {
      this.srcOutputDir = srcOutputDir;
      this.filesHeader = filesHeader;
      this.sqlResourceNamePrefix = sqlResourceNamePrefix;
   }

   @Override
   public void writeQueries
      (
         List<GeneratedQuery> generatedQueries,
         List<WrittenQueryReprPath> writtenQueryPaths,
         boolean includeTimestamp
      )
      throws IOException
   {
      if ( srcOutputDir != null )
         Files.createDirectories(srcOutputDir);

      for ( GeneratedQuery q : generatedQueries )
      {
         String moduleName = makeModuleName(q.getQueryName());

         @Nullable Path outputPath = getOutputFilePath(moduleName);

         BufferedWriter bw = IO.newFileOrStdoutWriter(outputPath);

         try
         {
            writeCommonSourceFileHeader(bw, includeTimestamp);

            writeQueryModuleFileHeaders(bw, q);

            bw.write("\n\n");

            writeQueryModuleMembers(bw, q, writtenQueryPaths);
         }
         finally
         {
            if ( outputPath != null ) bw.close();
            else bw.flush();
         }
      }
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

   private void writeQueryModuleFileHeaders
      (
         BufferedWriter bw,
         GeneratedQuery q
      )
      throws IOException
   {
      // Write common header for query files if specified.
      if ( filesHeader != null )
         bw.write(filesHeader + "\n");

      // Write any additional headers specified in the query.
      ifPresent(q.getTypesFileHeader(), hdr -> writeString(bw, hdr + "\n"));
   }

   private void writeQueryModuleMembers
      (
         BufferedWriter bw,
         GeneratedQuery q,
         List<WrittenQueryReprPath> writtenQueryPaths
      )
      throws IOException
   {
      writeQuerySqlFileReferenceMembers(bw, q, writtenQueryPaths);

      writeParamMembers(q.getParamNames(), bw, true);

      if ( !q.getGeneratedResultTypes().isEmpty() )
      {
         Set<String> writtenTypeNames = new HashSet<>();

         for ( GeneratedType generatedType: q.getGeneratedResultTypes() )
         {
            if ( !writtenTypeNames.contains(generatedType.getTypeName()) &&
                 !generatedType.isUnwrapped() )
            {
               bw.write('\n');
               bw.write(makeGeneratedTypeSource(generatedType));

               writtenTypeNames.add(generatedType.getTypeName());
            }
         }
      }
   }

   @SuppressWarnings("keyfor")
   private void writeQuerySqlFileReferenceMembers
      (
         BufferedWriter bw,
         GeneratedQuery q,
         List<WrittenQueryReprPath> writtenQueryPaths
      )
      throws IOException
   {
      Map<ResultsRepr,Path> sqlPathsByRepr = getWrittenSqlPathsForQuery(q.getQueryName(), writtenQueryPaths);

      // Write members holding resource/file names for the result representations that were written for this query.
      for ( ResultsRepr resultsRepr : sorted(sqlPathsByRepr.keySet()) )
      {
         String memberName = sqlPathsByRepr.size() == 1 ? "sqlResource" :
            "sqlResource" + upperCamelCase(resultsRepr.toString());
         String resourceName = sqlResourceNamePrefix + requireNonNull(sqlPathsByRepr.get(resultsRepr)).getFileName();
         bw.write("export const " + memberName + " = \"" + resourceName + "\";\n");
      }
      bw.write("\n");
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

   public String makeGeneratedTypeSource
      (
         GeneratedType genType
      )
   {
      StringBuilder sb = new StringBuilder();

      String typeName = genType.getTypeName();

      sb.append("export interface ");
      sb.append(typeName);
      sb.append("\n{\n");

      List<FieldInfo> fields = new ArrayList<>();
      genType.getDatabaseFields().forEach(f ->
         fields.add(new FieldInfo(f.getName(), getTSTypeNameForDatabaseField(f)))
      );
      genType.getExpressionFields().forEach(f ->
         fields.add(new FieldInfo(f.getName(), getTSTypeNameForExpressionField(f)))
      );
      genType.getChildCollectionFields().forEach(f ->
         fields.add(new FieldInfo(f.getName(), getChildCollectionDeclaredType(f)))
      );
      genType.getParentReferenceFields().forEach(f ->
         fields.add(new FieldInfo(f.getName(), getParentRefDeclaredType(f)))
      );

      // field declarations
      for ( FieldInfo f : fields )
      {
         sb.append("   ");
         sb.append(f.name);
         sb.append(": ");
         sb.append(f.typeDeclaration);
         sb.append(";\n");
      }

      sb.append("}\n");

      return sb.toString();
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

      @Nullable Path outputPath = getOutputFilePath("relations");

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

   private String getTSTypeNameForDatabaseField(DatabaseField f)
   {
      boolean notNull = !valueOr(f.getNullable(), true);

      @Nullable String typeOverride = f.getGeneratedFieldType();
      if ( typeOverride != null )
         return typeOverride;

      switch ( f.getJdbcTypeCode() )
      {
         case Types.TINYINT:
         case Types.SMALLINT:
         case Types.INTEGER:
         case Types.BIGINT:
         case Types.DECIMAL:
         case Types.NUMERIC:
         case Types.FLOAT:
         case Types.REAL:
         case Types.DOUBLE:
            return notNull ? "number" : "number | null";
         case Types.CHAR:
         case Types.VARCHAR:
         case Types.LONGVARCHAR:
         case Types.CLOB:
         case Types.DATE:
         case Types.TIME:
         case Types.TIMESTAMP:
         case Types.TIMESTAMP_WITH_TIMEZONE:
            return notNull ? "string" : "string | null";
         case Types.BIT:
         case Types.BOOLEAN:
            return notNull ? "boolean" : "boolean | null";
         case Types.OTHER:
            if ( f.getDatabaseType().toLowerCase().startsWith("json") )
               return notNull ? "any" : "any | null";
            else
               throw new RuntimeException("unsupported type for database field " + f);
         default:
            throw new RuntimeException("unsupported type for database field " + f);
      }
   }

   private String getTSTypeNameForExpressionField(ExpressionField f)
   {
      return valueOrThrow(f.getTypeDeclaration(), () ->
          new RuntimeException("Field type override is required for expression field " + f.getFieldExpression())
      );
   }


   private String getParentRefDeclaredType(ParentReferenceField parentRefField)
   {
      return
         !parentRefField.isNullable() ?
            parentRefField.getGeneratedType().getTypeName()
            : parentRefField.getGeneratedType().getTypeName() + " | null";
   }

   private String getChildCollectionDeclaredType(ChildCollectionField childCollField)
   {
      GeneratedType genType = childCollField.getGeneratedType();
      String elType = !genType.isUnwrapped() ? genType.getTypeName() : getSoleFieldDeclaredType(genType);
      String bareChildCollType = elType + "[]";
      return !childCollField.isNullable() ? bareChildCollType : bareChildCollType + " | null";
   }

   private String getSoleFieldDeclaredType(GeneratedType genType)
   {
      if ( genType.getFieldsCount() != 1 )
         throw new RuntimeException("Expected single field when unwrapping " + genType.getTypeName() + ".");

      if ( genType.getDatabaseFields().size() == 1 )
         return getTSTypeNameForDatabaseField(genType.getDatabaseFields().get(0));
      else if ( genType.getExpressionFields().size() == 1 )
         return getTSTypeNameForExpressionField(genType.getExpressionFields().get(0));
      else if ( genType.getChildCollectionFields().size() == 1 )
         return getChildCollectionDeclaredType(genType.getChildCollectionFields().get(0));
      else if ( genType.getParentReferenceFields().size() == 1 )
         return getParentRefDeclaredType(genType.getParentReferenceFields().get(0));
      throw
          new RuntimeException("Unhandled field category when unwrapping " + genType.getTypeName() + ".");
   }

   private @Nullable Path getOutputFilePath(String moduleName)
   {
      return applyIfPresent(srcOutputDir, d -> d.resolve(moduleName + ".ts"));
   }

   private List<ResultsRepr> sorted(Collection<ResultsRepr> xs)
   {
      return xs.stream().sorted().collect(toList());
   }

   private static String asStringLiteral(String s)
   {
      return "\"" + s.replace("\"", "\\\"") + "\"";
   }

   private static String makeModuleName(String statementName)
   {
      return statementName.replace(' ', '-').toLowerCase();
   }
}

