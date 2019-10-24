package gov.fda.nctr.sqljsonquery.types;

import java.util.*;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.sql.Types;
import static java.util.Collections.emptyMap;

import gov.fda.nctr.sqljsonquery.SqlJsonQuery;
import static gov.fda.nctr.util.Files.newFileOrStdoutWriter;
import static gov.fda.nctr.util.Optionals.opt;
import static gov.fda.nctr.util.StringFuns.camelCase;
import static gov.fda.nctr.util.StringFuns.indentLines;


public class JavaWriter implements SourceCodeWriter
{
   private String targetPackage;

   private Optional<Path> srcOutputBaseDir;

   // overridden field types by (query name, generated type name, field name in generated type)
   private Map<String,String> fieldTypeOverrides;

   public JavaWriter
   (
      String targetPackage,
      Optional<Path> srcOutputBaseDir,
      Optional<Map<String,String>> fieldTypeOverrides
   )
   {
      this.targetPackage = targetPackage;
      this.srcOutputBaseDir = srcOutputBaseDir;
      this.fieldTypeOverrides = new HashMap<>(fieldTypeOverrides.orElse(emptyMap()));
   }

   @Override
   public void writeSourceCode
   (
      List<SqlJsonQuery> generatedQueries
   )
      throws IOException
   {
      Optional<Path> outputDir = !targetPackage.isEmpty() ?
         srcOutputBaseDir.map(d -> d.resolve(targetPackage.replace('.','/')))
         : srcOutputBaseDir;

      for ( SqlJsonQuery sjq : generatedQueries )
      {
         String queryClassName = camelCase(sjq.getQueryName()) + "ResultTypes";
         Optional<Path> outputFilePath = outputDir.map(d -> d.resolve(queryClassName + ".java"));

         BufferedWriter bw = newFileOrStdoutWriter(outputFilePath);

         try
         {
            bw.write("// --------------------------------------------------------------------------\n");
            bw.write("// [ THIS SOURCE CODE WAS AUTO-GENERATED, ANY CHANGES MADE HERE MAY BE LOST. ]\n");
            bw.write("//   " + Instant.now().toString().replace('T',' ') + "\n");
            bw.write("// --------------------------------------------------------------------------\n");
            bw.write("package " + targetPackage + "\n\n");
            bw.write("import java.util.*;\n");
            bw.write("import java.math.*;\n");
            bw.write("import java.time.*;\n");

            bw.write("\n\n");
            bw.write("public class " + queryClassName + "\n");
            bw.write("{\n");

            for (GeneratedType generatedType: sjq.getGeneratedResultTypes() )
            {
               String srcCode = makeGeneratedTypeSource(generatedType, sjq.getQueryName());

               bw.write('\n');
               bw.write(indentLines(srcCode, 3));
               bw.write('\n');
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

   public String makeGeneratedTypeSource(GeneratedType generatedType, String queryName)
   {
      StringBuilder sb = new StringBuilder();

      String typeName = generatedType.getUnqualifiedClassName();

      sb.append("public static class ");
      sb.append(typeName);
      sb.append("\n{\n");

      for ( DatabaseField f : generatedType.getDatabaseFields() )
      {
         sb.append("   public ");
         sb.append(getJavaTypeNameForDatabaseField(f, queryName, typeName));
         sb.append(" ");
         sb.append(f.getName());
         sb.append(";\n");
      }

      for ( ChildCollectionField childCollField : generatedType.getChildCollectionFields() )
      {
         sb.append("   public List<");
         sb.append(childCollField.generatedType.getUnqualifiedClassName());
         sb.append("> ");
         sb.append(childCollField.getName());
         sb.append(";\n");
      }

      for ( ParentReferenceField parentRefField : generatedType.getParentReferenceFields() )
      {
         sb.append("   public ");
         sb.append(parentRefField.generatedType.getUnqualifiedClassName());
         sb.append(" ");
         sb.append(parentRefField.getName());
         sb.append(";\n");
      }

      sb.append("}\n");

      return sb.toString();
   }

   private String getJavaTypeNameForDatabaseField
   (
      DatabaseField f,
      String queryName,
      String generatedTypeName
   )
   {
      // Check if there's a type override specified for this field.
      String typeOverrideKey = queryName + "/" + generatedTypeName + "." + f.getName();
      if ( fieldTypeOverrides.containsKey(typeOverrideKey) )
         return fieldTypeOverrides.get(typeOverrideKey);

      switch (f.getJdbcTypeCode())
      {
         case Types.TINYINT:
         case Types.SMALLINT:
            return "int";
         case Types.INTEGER:
         case Types.BIGINT:
            return !f.getPrecision().isPresent() || f.getPrecision().get() > 9 ? "long": "int";
         case Types.DECIMAL:
         case Types.NUMERIC:
            if ( f.getFractionalDigits().equals(opt(0) ) )
               return !f.getPrecision().isPresent() || f.getPrecision().get() > 9 ? "long": "int";
            else
               return "BigDecimal";
         case Types.FLOAT:
         case Types.REAL:
         case Types.DOUBLE:
            return "double";
         case Types.CHAR:
         case Types.VARCHAR:
         case Types.LONGVARCHAR:
         case Types.CLOB:
            return "String";
         case Types.BIT:
         case Types.BOOLEAN:
            return "boolean";
         case Types.DATE:
            return "LocalDate";
         case Types.TIME:
            return "LocalTime";
         case Types.TIMESTAMP:
            return "Instant";
         case Types.OTHER:
            if ( f.getDatabaseType().toLowerCase().startsWith("json") )
               return "String";
            else
               throw new RuntimeException("unsupported type for database field " + f);
         default:
            throw new RuntimeException("unsupported type for database field " + f);
      }
   }

}
