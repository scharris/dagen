package org.sqljsonquery.types.source_writers;

import java.nio.file.Files;
import java.util.*;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.sql.Types;

import org.sqljsonquery.SqlJsonQuery;
import org.sqljsonquery.WrittenQueryReprPath;
import org.sqljsonquery.queryspec.FieldTypeOverride;
import org.sqljsonquery.queryspec.ResultsRepr;
import org.sqljsonquery.queryspec.TableOutputField;
import org.sqljsonquery.types.*;
import static org.sqljsonquery.types.source_writers.JavaWriter.NullableFieldRepr.*;
import static org.sqljsonquery.util.Files.newFileOrStdoutWriter;
import static org.sqljsonquery.util.Optionals.opt;
import static org.sqljsonquery.util.StringFuns.*;


public class JavaWriter implements SourceCodeWriter
{
   private String targetPackage;
   private Optional<Path> srcOutputBaseDir;
   private NullableFieldRepr nullableFieldRepr;
   private Optional<String> filesHeader;

   public enum NullableFieldRepr { OPTWRAPPED, ANNOTATED, BARETYPE }

   public JavaWriter
   (
      String targetPackage,
      Optional<Path> srcOutputBaseDir,
      NullableFieldRepr nullableFieldRepr,
      Optional<String> filesHeader
   )
   {
      this.targetPackage = targetPackage;
      this.srcOutputBaseDir = srcOutputBaseDir;
      this.nullableFieldRepr = nullableFieldRepr;
      this.filesHeader = filesHeader;
   }

   @Override
   public void writeSourceCode
   (
      List<SqlJsonQuery> generatedQueries,
      List<WrittenQueryReprPath> writtenQueryPaths
   )
      throws IOException
   {
      Optional<Path> outputDir = !targetPackage.isEmpty() ?
         srcOutputBaseDir.map(d -> d.resolve(targetPackage.replace('.','/')))
         : srcOutputBaseDir;

      if ( outputDir.isPresent() )
         Files.createDirectories(outputDir.get());

      for ( SqlJsonQuery sjq : generatedQueries )
      {
         String queryClassName = upperCamelCase(sjq.getQueryName()) + "ResultTypes";
         Optional<Path> outputFilePath = outputDir.map(d -> d.resolve(queryClassName + ".java"));

         BufferedWriter bw = newFileOrStdoutWriter(outputFilePath);

         Map<ResultsRepr,Path> writtenQueryPathsByRepr =
            WrittenQueryReprPath.writtenPathsForQuery(sjq.getQueryName(), writtenQueryPaths);

         try
         {
            bw.write("// --------------------------------------------------------------------------\n");
            bw.write("// [ THIS SOURCE CODE WAS AUTO-GENERATED, ANY CHANGES MADE HERE MAY BE LOST. ]\n");
            bw.write("//   " + Instant.now().toString().replace('T',' ') + "\n");
            bw.write("// --------------------------------------------------------------------------\n");
            bw.write("package " + targetPackage + ";\n\n");
            bw.write("import java.util.*;\n");
            bw.write("import java.math.*;\n");
            bw.write("import java.time.*;\n");
            bw.write("import com.fasterxml.jackson.databind.JsonNode;\n");

            if ( filesHeader.isPresent() ) bw.write(filesHeader.get());

            bw.write("\n\n");
            bw.write("public class " + queryClassName + "\n");
            bw.write("{\n");

            // Write members holding resource/file names for the result representations that were written for this query.
            for ( ResultsRepr resultsRepr : writtenQueryPathsByRepr.keySet() )
            {
               String memberName = writtenQueryPathsByRepr.size() == 1 ? "sqlResourceName" :
                  "sqlResourceName" + upperCamelCase(resultsRepr.toString());
               String resourceName = writtenQueryPathsByRepr.get(resultsRepr).getFileName().toString();
               bw.write("   public static final String " + memberName + " = \"" + resourceName + "\";\n");
            }
            bw.write("\n");

            String topClass = sjq.getGeneratedResultTypes().get(0).getTypeName();
            bw.write("   public static final Class<" + topClass + "> principalResultClass = " + topClass + ".class;\n\n");

            Set<String> writtenTypeNames = new HashSet<>();

            for ( GeneratedType generatedType: sjq.getGeneratedResultTypes() )
            {
               if ( !writtenTypeNames.contains(generatedType.getTypeName()) )
               {
                  String srcCode = makeGeneratedTypeSource(generatedType);

                  bw.write('\n');
                  bw.write(indentLines(srcCode, 3));
                  bw.write('\n');

                  writtenTypeNames.add(generatedType.getTypeName());
               }
            }

            bw.write("}\n");
         }
         finally
         {
            if ( outputFilePath.isPresent() ) bw.close();
            else bw.flush();
         }
      }
   }

   private String makeGeneratedTypeSource(GeneratedType generatedType)
   {
      StringBuilder sb = new StringBuilder();

      String typeName = generatedType.getTypeName();

      sb.append("public static class ");
      sb.append(typeName);
      sb.append("\n{\n");

      for ( DatabaseField f : generatedType.getDatabaseFields() )
      {
         sb.append("   public ");
         sb.append(getJavaTypeNameForDatabaseField(f));
         sb.append(" ");
         sb.append(f.getName());
         sb.append(";\n");
      }

      for ( TableOutputField tof : generatedType.getExpressionFields() )
      {
         String fieldName = tof.getOutputName().orElseThrow(() ->
            new RuntimeException("Output name is required for expression field " + tof.getFieldExpression())
         );
         FieldTypeOverride typeOverride = tof.getTypeOverride("Java").orElseThrow(() ->
            new RuntimeException("Field type override is required for expression field " + tof.getFieldExpression())
         );
         sb.append("   public ");
         sb.append(typeOverride.getTypeDeclaration());
         sb.append(" ");
         sb.append(fieldName);
         sb.append(";\n");
      }

      for ( ChildCollectionField childCollField : generatedType.getChildCollectionFields() )
      {
         sb.append("   public ");
         sb.append(getChildCollectionDeclaredType(childCollField));
         sb.append(" ");
         sb.append(childCollField.getName());
         sb.append(";\n");
      }

      for ( ParentReferenceField parentRefField : generatedType.getParentReferenceFields() )
      {
         sb.append("   public ");
         sb.append(getParentRefDeclaredType(parentRefField));
         sb.append(" ");
         sb.append(parentRefField.getName());
         sb.append(";\n");
      }

      sb.append("}\n");

      return sb.toString();
   }

   private String getJavaTypeNameForDatabaseField(DatabaseField f)
   {
      boolean notNull = !(f.getNullable().orElse(true));

      Optional<FieldTypeOverride> typeOverride = f.getTypeOverride("Java");
      if ( typeOverride.isPresent() )
         return typeOverride.get().getTypeDeclaration();

      switch ( f.getJdbcTypeCode() )
      {
         case Types.TINYINT:
         case Types.SMALLINT:
            return notNull ? "int" : nullableType("Integer");
         case Types.INTEGER:
         case Types.BIGINT:
         {
            boolean needsLong = !f.getPrecision().isPresent() || f.getPrecision().get() > 9;
            if ( notNull ) return needsLong ? "long": "int";
            else return needsLong ? nullableType("Long"): nullableType("Integer");
         }
         case Types.DECIMAL:
         case Types.NUMERIC:
            if ( f.getFractionalDigits().equals(opt(0) ) )
            {
               boolean needsLong = !f.getPrecision().isPresent() || f.getPrecision().get() > 9;
               if ( notNull ) return needsLong ? "long": "int";
               else return needsLong ? nullableType("Long"): nullableType("Integer");
            }
            else
               return notNull ? "BigDecimal" : nullableType("BigDecimal");
         case Types.FLOAT:
         case Types.REAL:
         case Types.DOUBLE:
            return notNull ? "double" : nullableType("Double");
         case Types.CHAR:
         case Types.VARCHAR:
         case Types.LONGVARCHAR:
         case Types.CLOB:
            return notNull ? "String" : nullableType("String");
         case Types.BIT:
         case Types.BOOLEAN:
            return notNull ? "boolean" : nullableType("Boolean");
         case Types.DATE:
            return notNull ? "LocalDate" : nullableType("LocalDate");
         case Types.TIME:
            return notNull ? "LocalTime" : nullableType("LocalTime");
         case Types.TIMESTAMP:
            return notNull ? "Instant" : nullableType("Instant");
         case Types.OTHER:
            if ( f.getDatabaseType().toLowerCase().startsWith("json") )
               return notNull ? "JsonNode" : nullableType("JsonNode");
            else
               throw new RuntimeException("unsupported type for database field " + f);
         default:
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
      String bareChildCollType = "List<" + childCollField.getGeneratedType().getTypeName() + ">";
      return !childCollField.isNullable() ? bareChildCollType : nullableType(bareChildCollType);
   }

   private String nullableType(String baseType)
   {
      StringBuilder sb = new StringBuilder();

      if ( nullableFieldRepr == ANNOTATED )
         sb.append("@Null ");
      else if ( nullableFieldRepr == OPTWRAPPED  )
         sb.append("Optional<");
      else if ( nullableFieldRepr != BARETYPE )
         throw new RuntimeException("unexpected nullable field repr: " + nullableFieldRepr);

      sb.append(baseType);

      if ( nullableFieldRepr == OPTWRAPPED  )
         sb.append(">");

      return sb.toString();
   }

}
