package org.sqljson.common.sql_dialects;

import java.util.List;
import static java.util.stream.Collectors.joining;

import org.sqljson.queries.sql.ColumnMetadata;
import org.sqljson.common.util.StringFuns;


public class PostgresDialect implements SqlDialect
{
   private final int indentSpaces;

   public PostgresDialect(int indentSpaces)
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
         .map(col -> "'" + StringFuns.unDoubleQuote(col.getName()) + "', " + fromAlias + "." + col.getName())
         .collect(joining(",\n"));

         return
            "jsonb_build_object(\n" +
               StringFuns.indentLines(objectFieldDecls, indentSpaces) + "\n" +
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
      return "coalesce(jsonb_agg(" + rowObjExpr + "),'[]'::jsonb)";
   }

   @Override
   public String getAggregatedColumnValuesExpression
      (
          ColumnMetadata columnMetadata,
          String fromAlias
      )
   {
      String qfield = fromAlias + "." + columnMetadata.getName();
      return "coalesce(jsonb_agg(" + qfield + "))";
   }
}

