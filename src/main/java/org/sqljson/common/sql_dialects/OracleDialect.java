package org.sqljson.common.sql_dialects;

import java.util.List;

import static java.util.stream.Collectors.joining;

import org.sqljson.queries.sql.ColumnMetadata;

import static org.sqljson.common.util.StringFuns.*;


public class OracleDialect implements SqlDialect
{
   private final int indentSpaces;

   public OracleDialect(int indentSpaces)
   {
      this.indentSpaces = indentSpaces;
   }

   @Override
   public String getRowObjectExpression
      (
         List<ColumnMetadata> columnMetadatas,
         String fromAlias
      )
   {
      String objectFieldDecls =
         columnMetadatas.stream()
         .map(col -> "'" + unDoubleQuote(col.getName()) + "' value " + fromAlias + "." + col.getName())
         .collect(joining(",\n"));

      return
         "json_object(\n" +
            indentLines(objectFieldDecls, indentSpaces) + "\n" +
            "  returning clob\n" +
         ")";
   }

   @Override
   public String getAggregatedRowObjectsExpression
      (
         List<ColumnMetadata> columnMetadatas,
         String fromAlias
      )
   {
      String rowObjExpr = getRowObjectExpression(columnMetadatas, fromAlias);
      return "treat(coalesce(json_arrayagg(" + rowObjExpr + " returning clob), to_clob('[]')) as json)";
   }

   @Override
   public String getAggregatedColumnValuesExpression
      (
         ColumnMetadata columnMetadata,
         String fromAlias
      )
   {
      String qfield = fromAlias + "." + columnMetadata.getName();
      return "treat(coalesce(json_arrayagg(" + qfield + " returning clob), to_clob('[]')) as json)";
   }
}

