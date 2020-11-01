package org.sqljson.queries.source_writers;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Types;
import java.time.Instant;
import java.util.*;
import java.util.regex.Pattern;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

import org.checkerframework.checker.nullness.qual.Nullable;

import org.sqljson.queries.GeneratedQuery;
import org.sqljson.queries.WrittenQueryReprPath;
import org.sqljson.queries.result_types.*;
import org.sqljson.queries.specs.ResultsRepr;
import static org.sqljson.queries.WrittenQueryReprPath.getWrittenSqlPathsForQuery;
import static org.sqljson.common.util.IO.newFileOrStdoutWriter;
import static org.sqljson.common.util.IO.writeString;
import static org.sqljson.common.util.Nullables.*;
import static org.sqljson.common.util.StringFuns.*;


public class JavaWriter implements SourceCodeWriter
{
   private final String targetPackage;
   private final @Nullable Path packageOutputDir;
   private final NullableFieldRepr nullableFieldRepr;
   private final @Nullable String filesHeader;
   private final String sqlResourceNamePrefix;
   private final boolean generateGetters;
   private final boolean generateSetters;

   private final Pattern TIMESTAMPTZ_REGEX = Pattern.compile("^timestamp(\\([0-9]+\\))? with time zone$");
   public enum NullableFieldRepr { OPTWRAPPED, ANNOTATED, BARETYPE }

   public JavaWriter
      (
         String targetPackage,
         @Nullable Path srcOutputBaseDir,
         NullableFieldRepr nullableFieldRepr,
         @Nullable String filesHeader,
         @Nullable String sqlResourceNamePrefix,
         boolean generateGetters,
         boolean generateSetters
      )
   {
      this.packageOutputDir = !targetPackage.isEmpty() ?
         applyIfPresent(srcOutputBaseDir, d -> d.resolve(targetPackage.replace('.','/')))
         : srcOutputBaseDir;
      this.targetPackage = targetPackage;
      this.nullableFieldRepr = nullableFieldRepr;
      this.filesHeader = filesHeader;
      this.sqlResourceNamePrefix = valueOr(sqlResourceNamePrefix, "");
      this.generateGetters = generateGetters;
      this.generateSetters = generateSetters;

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
      if ( packageOutputDir != null )
         Files.createDirectories(packageOutputDir);

      for ( GeneratedQuery q : generatedQueries )
      {
         String queryClassName = upperCamelCase(q.getQueryName());

         @Nullable Path outputPath = getOutputFilePath(queryClassName);

         BufferedWriter bw = newFileOrStdoutWriter(outputPath);

         try
         {
            writeCommonHeaderAndPackageDeclaration(bw, includeTimestamp);

            writeQueryFileImportsAndHeaders(bw, q);

            bw.write("\n\n");

            writeQueryClass(bw, queryClassName, q, writtenQueryPaths);
         }
         finally
         {
            if ( outputPath != null ) bw.close();
            else bw.flush();
         }
      }
   }

   private void writeQueryFileImportsAndHeaders
      (
         BufferedWriter bw,
         GeneratedQuery q
      )
      throws IOException
   {
      bw.write("import java.util.*;\n");
      bw.write("import java.math.*;\n");
      bw.write("import java.time.*;\n");
      if ( nullableFieldRepr == NullableFieldRepr.ANNOTATED ) bw.write(
          "import org.checkerframework.checker.nullness.qual.Nullable;\n" +
         "import org.checkerframework.checker.nullness.qual.NonNull;\n" +
         "import org.checkerframework.framework.qual.DefaultQualifier;\n" +
         "import org.checkerframework.framework.qual.TypeUseLocation;\n"
      );
      bw.write("import com.fasterxml.jackson.databind.JsonNode;\n");
      bw.write("import com.fasterxml.jackson.databind.node.*;\n");

      // Write common headers if any.
      if ( filesHeader != null )
         bw.write(filesHeader + "\n");

      // Write any additional headers specified in the query.
      ifPresent(q.getTypesFileHeader(), hdr -> writeString(bw, hdr + "\n"));
   }

   private void writeQueryClass
      (
         BufferedWriter bw,
         String queryClassName,
         GeneratedQuery q,
         List<WrittenQueryReprPath> writtenQueryPaths
      )
      throws IOException
   {
      bw.write("public class " + queryClassName + "\n");
      bw.write("{\n");

      writeQuerySqlFileReferenceMembers(bw, q, writtenQueryPaths);

      writeParamMembers(q.getParamNames(), bw, true);

      if ( !q.getGeneratedResultTypes().isEmpty() )
      {
         String topClass = q.getGeneratedResultTypes().get(0).getTypeName();

         bw.write("   public static final Class<" + topClass + "> principalResultClass = " +
                  topClass + ".class;\n\n");

         Set<String> writtenTypeNames = new HashSet<>();

         for ( GeneratedType generatedType: q.getGeneratedResultTypes() )
         {
            if ( !writtenTypeNames.contains(generatedType.getTypeName()) &&
                 !generatedType.isUnwrapped() )
            {
               String srcCode = makeGeneratedTypeSource(generatedType);

               bw.write('\n');
               bw.write(indentLines(srcCode, 3));
               bw.write('\n');

               writtenTypeNames.add(generatedType.getTypeName());
            }
         }
      }

      bw.write("}\n");
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
         bw.write("   public static final String " + memberName + " = \"" + resourceName + "\";\n");
      }
      bw.write("\n");
   }

   private void writeCommonHeaderAndPackageDeclaration
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
      if ( !targetPackage.isEmpty() )
         bw.write("package " + targetPackage + ";\n\n");
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

   private @Nullable Path getOutputFilePath(String className)
   {
      return applyIfPresent(packageOutputDir, d -> d.resolve(className + ".java"));
   }

   private String makeGeneratedTypeSource(GeneratedType genType)
   {
      StringBuilder sb = new StringBuilder();

      String typeName = genType.getTypeName();

      if ( nullableFieldRepr == NullableFieldRepr.ANNOTATED ) sb.append(
         "@DefaultQualifier(value=NonNull.class)\n" +
         "@SuppressWarnings(\"nullness\") // because fields will be set directly by the deserializer not by constructor\n"
      );
      sb.append("public static class ");
      sb.append(typeName);
      sb.append("\n{\n");

      List<FieldInfo> fields = new ArrayList<>();
      genType.getDatabaseFields().forEach(f ->
         fields.add(new FieldInfo(f.getName(), getJavaTypeNameForDatabaseField(f)))
      );
      genType.getExpressionFields().forEach(f ->
         fields.add(new FieldInfo(f.getName(), getJavaTypeNameForExpressionField(f)))
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
         sb.append("   public ");
         sb.append(f.typeDeclaration);
         sb.append(" ");
         sb.append(f.name);
         sb.append(";\n");
      }

      if ( generateGetters || generateSetters )
      {
         sb.append("\n");

         // getters and setters
         for ( FieldInfo f : fields )
         {
            if ( generateGetters )
            {
               sb.append("   public ");
               sb.append(f.typeDeclaration);
               sb.append(" get");
               sb.append(capitalize(f.name));
               sb.append("() { return ");
               sb.append(f.name);
               sb.append("; }\n");
            }

            if ( generateSetters )
            {
               sb.append("   public void ");
               sb.append(" set");
               sb.append(capitalize(f.name));
               sb.append("(");
               sb.append(f.typeDeclaration);
               sb.append(" v) { ");
               sb.append(f.name);
               sb.append(" = v; }\n");
            }
         }
      }

      sb.append("}\n");

      return sb.toString();
   }

   private String getJavaTypeNameForExpressionField(ExpressionField f)
   {
      return valueOrThrow(f.getTypeDeclaration(), () ->
          new RuntimeException("Field type override is required for expression field " + f.getFieldExpression())
      );
   }

   private String getJavaTypeNameForDatabaseField(DatabaseField f)
   {
      return getJavaTypeNameForDatabaseField(f, false);
   }

   private String getJavaTypeNameForDatabaseField
      (
          DatabaseField f,
          boolean box
      )
   {
      boolean notNull = !valueOr(f.getNullable(), true);

      @Nullable String typeDecl = f.getGeneratedFieldType();
      if ( typeDecl != null )
         return typeDecl;

      switch ( f.getJdbcTypeCode() )
      {
         case Types.TINYINT:
         case Types.SMALLINT:
            return notNull ? (box ? "Integer" : "int") : nullableType("Integer");
         case Types.INTEGER:
         case Types.BIGINT:
         {
            boolean needsLong = applyOr(f.getPrecision(), prec -> prec > 9, true);
            if ( notNull ) return needsLong ? (box ? "Long" : "long") : (box ? "Integer" : "int");
            else return needsLong ? nullableType("Long"): nullableType("Integer");
         }
         case Types.DECIMAL:
         case Types.NUMERIC:
            if ( Objects.equals(f.getFractionalDigits(), 0) )
            {
               boolean needsLong = applyOr(f.getPrecision(), prec -> prec > 9, true);
               if ( notNull ) return needsLong ? (box ? "Long" : "long") : (box ? "Integer" : "int");
               else return needsLong ? nullableType("Long"): nullableType("Integer");
            }
            else
               return notNull ? "BigDecimal" : nullableType("BigDecimal");
         case Types.FLOAT:
         case Types.REAL:
         case Types.DOUBLE:
            return notNull ? (box ? "Double": "double") : nullableType("Double");
         case Types.CHAR:
         case Types.VARCHAR:
         case Types.LONGVARCHAR:
         case Types.CLOB:
            return notNull ? "String" : nullableType("String");
         case Types.BIT:
         case Types.BOOLEAN:
            return notNull ? (box ? "Boolean" : "boolean") : nullableType("Boolean");
         case Types.DATE:
            return notNull ? "LocalDate" : nullableType("LocalDate");
         case Types.TIME:
            return notNull ? "LocalTime" : nullableType("LocalTime");
         case Types.TIMESTAMP:
         case Types.TIMESTAMP_WITH_TIMEZONE:
            return notNull ? "OffsetDateTime" : nullableType("OffsetDateTime");
         default:
            if ( f.getDatabaseType().toLowerCase().startsWith("json") )
               return notNull ? "JsonNode" : nullableType("JsonNode");
            // Another entry for timezones, this entry is for Oracle drivers that don't
            // declare timestamps with timezone with the proper java.sql.Types constant.
            else if ( TIMESTAMPTZ_REGEX.matcher(f.getDatabaseType().toLowerCase()).matches() )
               return notNull ? "OffsetDateTime" : nullableType("OffsetDateTime");
            throw new RuntimeException("unsupported type for database field " + f);
      }
   }

   private String getParentRefDeclaredType(ParentReferenceField parentRefField)
   {
      return
         !parentRefField.isNullable() ?
            parentRefField.getGeneratedType().getTypeName()
            : nullableType(parentRefField.getGeneratedType().getTypeName());
   }

   private String getChildCollectionDeclaredType(ChildCollectionField childCollField)
   {
      GeneratedType genType = childCollField.getGeneratedType();
      String elType = !genType.isUnwrapped() ? genType.getTypeName() : getSoleFieldDeclaredBoxedType(genType);
      String bareChildCollType = "List<" + elType + ">";
      return !childCollField.isNullable() ? bareChildCollType : nullableType(bareChildCollType);
   }

   private String getSoleFieldDeclaredBoxedType(GeneratedType genType)
   {
      if ( genType.getFieldsCount() != 1 )
         throw new RuntimeException("Expected single field when unwrapping " + genType.getTypeName() + ".");

      if ( genType.getDatabaseFields().size() == 1 )
         return getJavaTypeNameForDatabaseField(genType.getDatabaseFields().get(0), true);
      else if ( genType.getExpressionFields().size() == 1 )
         return getJavaTypeNameForExpressionField(genType.getExpressionFields().get(0));
      else if ( genType.getChildCollectionFields().size() == 1 )
         return getChildCollectionDeclaredType(genType.getChildCollectionFields().get(0));
      else if ( genType.getParentReferenceFields().size() == 1 )
         return getParentRefDeclaredType(genType.getParentReferenceFields().get(0));
      throw
          new RuntimeException("Unhandled field category when unwrapping " + genType.getTypeName() + ".");
   }

   private String nullableType(String baseType)
   {
      StringBuilder sb = new StringBuilder();

      if ( nullableFieldRepr == NullableFieldRepr.ANNOTATED )
         sb.append("@Nullable ");
      else if ( nullableFieldRepr == NullableFieldRepr.OPTWRAPPED  )
         sb.append("Optional<");
      else if ( nullableFieldRepr != NullableFieldRepr.BARETYPE )
         throw new RuntimeException("unexpected nullable field repr: " + nullableFieldRepr);

      sb.append(baseType);

      if ( nullableFieldRepr == NullableFieldRepr.OPTWRAPPED  )
         sb.append(">");

      return sb.toString();
   }

   private List<ResultsRepr> sorted(Collection<ResultsRepr> xs)
   {
      return xs.stream().sorted().collect(toList());
   }
}

class FieldInfo
{
   String name;
   String typeDeclaration;

   public FieldInfo(String name, String typeDeclaration)
   {
      this.name = name;
      this.typeDeclaration = typeDeclaration;
   }
}
