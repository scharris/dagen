package org.sqljson.result_types.source_writers;

import java.nio.file.Files;
import java.util.*;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.sql.Types;

import org.sqljson.GeneratedQuery;
import org.sqljson.result_types.*;
import org.sqljson.specs.queries.FieldTypeOverride;
import org.sqljson.specs.queries.ResultsRepr;
import org.sqljson.util.StringFuns;
import org.sqljson.WrittenQueryReprPath;


public class TypescriptWriter implements SourceCodeWriter
{
   private Optional<Path> srcOutputDir;
   private Optional<String> filesHeader;

   public TypescriptWriter
   (
      Optional<Path> srcOutputDir,
      Optional<String> filesHeader
   )
   {
      this.srcOutputDir = srcOutputDir;
      this.filesHeader = filesHeader;
   }

   @Override
   public void writeSourceCode
   (
      List<GeneratedQuery> generatedQueries,
      List<WrittenQueryReprPath> writtenQueryPaths,
      boolean includeTimestamp
   )
      throws IOException
   {
      if ( srcOutputDir.isPresent() )
         Files.createDirectories(srcOutputDir.get());

      for ( GeneratedQuery q : generatedQueries )
      {
         String moduleName = StringFuns.upperCamelCase(q.getName()) + "ResultTypes";
         Optional<Path> outputFilePath = srcOutputDir.map(d -> d.resolve(moduleName + ".ts"));

         BufferedWriter bw = org.sqljson.util.Files.newFileOrStdoutWriter(outputFilePath);

         Map<ResultsRepr,Path> writtenQueryPathsByRepr =
            WrittenQueryReprPath.writtenPathsForQuery(q.getName(), writtenQueryPaths);

         try
         {
            bw.write("// --------------------------------------------------------------------------\n");
            bw.write("// [ THIS SOURCE CODE WAS AUTO-GENERATED, ANY CHANGES MADE HERE MAY BE LOST. ]\n");
            if ( includeTimestamp )
               bw.write("//   " + Instant.now().toString().replace('T',' ') + "\n");
            bw.write("// --------------------------------------------------------------------------\n");

            if ( filesHeader.isPresent() )
               bw.write(filesHeader.get());

            bw.write("\n\n");

            // Write members holding resource/file names for the result representations that were written for this query.
            for ( ResultsRepr resultsRepr : writtenQueryPathsByRepr.keySet() )
            {
               String memberName = writtenQueryPathsByRepr.size() == 1 ? "sqlResourceName" :
                  "sqlResourceName" + StringFuns.upperCamelCase(resultsRepr.toString());
               String resourceName = writtenQueryPathsByRepr.get(resultsRepr).getFileName().toString();
               bw.write("export const " + memberName + " = \"" + resourceName + "\";\n");
            }
            bw.write("\n");

            String topTypeName = q.getGeneratedResultTypes().get(0).getTypeName();
            bw.write("export const principalResultType = '" + topTypeName + "';\n\n");

            Set<String> writtenTypeNames = new HashSet<>();

            for ( GeneratedType generatedType: q.getGeneratedResultTypes() )
            {
               if ( !writtenTypeNames.contains(generatedType.getTypeName()) )
               {
                  String srcCode = makeGeneratedTypeSource(generatedType);

                  bw.write('\n');
                  bw.write(srcCode);
                  bw.write('\n');

                  writtenTypeNames.add(generatedType.getTypeName());
               }
            }
         }
         finally
         {
            if ( outputFilePath.isPresent() ) bw.close();
            else bw.flush();
         }
      }
   }

   public String makeGeneratedTypeSource(GeneratedType generatedType)
   {
      StringBuilder sb = new StringBuilder();

      String typeName = generatedType.getTypeName();

      sb.append("export interface ");
      sb.append(typeName);
      sb.append("\n{\n");

      for ( DatabaseField f : generatedType.getDatabaseFields() )
      {
         sb.append("   ");
         sb.append(f.getName());
         sb.append(": ");
         sb.append(getTSTypeNameForDatabaseField(f));
         sb.append(";\n");
      }

      for ( ChildCollectionField childCollField : generatedType.getChildCollectionFields() )
      {
         sb.append("   ");
         sb.append(childCollField.getName());
         sb.append(": ");
         sb.append(getChildCollectionDeclaredType(childCollField));
         sb.append(";\n");
      }

      for ( ParentReferenceField parentRefField : generatedType.getParentReferenceFields() )
      {
         sb.append("   ");
         sb.append(parentRefField.getName());
         sb.append(": ");
         sb.append(getParentRefDeclaredType(parentRefField));
         sb.append(";\n");
      }

      sb.append("}\n");

      return sb.toString();
   }

   private String getTSTypeNameForDatabaseField(DatabaseField f)
   {
      boolean notNull = !(f.getNullable().orElse(true));

      Optional<FieldTypeOverride> typeOverride = f.getTypeOverride("Typescript");
      if ( typeOverride.isPresent() )
         return typeOverride.get().getTypeDeclaration();

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
            return notNull ? "number" : "number?";
         case Types.CHAR:
         case Types.VARCHAR:
         case Types.LONGVARCHAR:
         case Types.CLOB:
         case Types.DATE:
         case Types.TIME:
         case Types.TIMESTAMP:
            return notNull ? "string" : "string?";
         case Types.BIT:
         case Types.BOOLEAN:
            return notNull ? "boolean" : "boolean?";
         case Types.OTHER:
            if ( f.getDatabaseType().toLowerCase().startsWith("json") )
               return notNull ? "any" : "any?";
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
            : parentRefField.getGeneratedType().getTypeName() + "?";
   }

   private String getChildCollectionDeclaredType(ChildCollectionField childCollField)
   {
      String bareChildCollType = childCollField.getGeneratedType().getTypeName() + "[]";
      return !childCollField.isNullable() ? bareChildCollType : bareChildCollType + "?";
   }
}
