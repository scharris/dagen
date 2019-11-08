package org.sqljson.sql.dialect;

import java.util.List;
import static java.util.stream.Collectors.joining;

import org.sqljson.sql.ColumnMetadata;
import org.sqljson.util.StringFuns;


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
         .map(col -> "'" + StringFuns.unDoubleQuote(col.getOutputName()) + "', " + fromAlias + "." + col.getOutputName())
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
      return
         "coalesce(jsonb_agg(" +
            getRowObjectExpression(columnMetadatas, fromAlias) +
         "),'[]'::jsonb)";
   }

   @Override
   public String getAggregatedObjectsFinalQuery(String simpleAggregateQuery, String jsonValueColumnName)
   {
      return simpleAggregateQuery; // no correction necessary
   }
}
