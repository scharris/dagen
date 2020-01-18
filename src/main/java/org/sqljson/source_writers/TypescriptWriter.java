package org.sqljson.source_writers;

import java.nio.file.Files;
import java.util.*;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.sql.Types;

import org.sqljson.GeneratedModStatement;
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
   private String sqlResourceNamePrefix;

   public TypescriptWriter
   (
      Optional<Path> srcOutputDir,
      Optional<String> filesHeader,
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
      if ( srcOutputDir.isPresent() )
         Files.createDirectories(srcOutputDir.get());

      for ( GeneratedQuery q : generatedQueries )
      {
         String moduleName = StringFuns.upperCamelCase(q.getQueryName());
         Optional<Path> outputFilePath = srcOutputDir.map(d -> d.resolve(moduleName + ".ts"));

         BufferedWriter bw = org.sqljson.util.Files.newFileOrStdoutWriter(outputFilePath);

         Map<ResultsRepr,Path> writtenQueryPathsByRepr =
            WrittenQueryReprPath.writtenPathsForQuery(q.getQueryName(), writtenQueryPaths);

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
               String memberName = writtenQueryPathsByRepr.size() == 1 ? "sqlResource" :
                  "sqlResource" + StringFuns.upperCamelCase(resultsRepr.toString());
               String resourceName =
                  sqlResourceNamePrefix + writtenQueryPathsByRepr.get(resultsRepr).getFileName().toString();
               bw.write("export const " + memberName + " = \"" + resourceName + "\";\n");
            }
            bw.write("\n");

            if ( !q.getGeneratedResultTypes().isEmpty() )
            {
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
         }
         finally
         {
            if ( outputFilePath.isPresent() ) bw.close();
            else bw.flush();
         }
      }
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
      // TODO
      throw new RuntimeException("Typescript source generation for mod statements is not yet implemented.");
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

      for ( ExpressionField f : generatedType.getExpressionFields() )
      {
         sb.append("   ");
         sb.append(f.getName());
         sb.append(": ");
         sb.append(getTSTypeNameForExpressionField(f));
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

   private String getTSTypeNameForExpressionField(ExpressionField f)
   {
      FieldTypeOverride typeOverride = f.getTypeOverride("Typescript").orElseThrow(() ->
          new RuntimeException("Field type override is required for expression field " + f.getFieldExpression())
      );
      return typeOverride.getTypeDeclaration();
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
      GeneratedType genType = childCollField.getGeneratedType();
      String elType = !genType.isUnwrapped() ? genType.getTypeName() : getSoleFieldDeclaredType(genType);
      String bareChildCollType = elType + "[]";
      return !childCollField.isNullable() ? bareChildCollType : bareChildCollType + "?";
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
}
