package org.sqljsonquery.sql.dialect;

import java.util.List;
import static java.util.stream.Collectors.joining;

import static org.sqljsonquery.util.StringFuns.indentLines;
import static org.sqljsonquery.util.StringFuns.unDoubleQuote;


public class PostgresDialect implements SqlDialect
{
   private final int indentSpaces;

   public PostgresDialect(int indentSpaces)
   {
      this.indentSpaces = indentSpaces;
   }

   public String getJsonObjectSelectExpression
   (
      List<String> fromColumnNames,
      String fromAlias
   )
   {
      String objectFieldDecls =
         fromColumnNames.stream()
         .map(col -> "'" + unDoubleQuote(col) + "', " + fromAlias + "." + col)
         .collect(joining(",\n"));

         return
            "jsonb_build_object(\n" +
               indentLines(objectFieldDecls, indentSpaces) + "\n" +
            ")";
   }

   public String getJsonAggregatedObjectsSelectExpression
   (
      List<String> fromColumnNames,
      String fromAlias
   )
   {
      String objectFieldDecls =
         fromColumnNames.stream()
         .map(col -> "'" + unDoubleQuote(col) + "', " + fromAlias + "." + col)
         .collect(joining(",\n"));

      return
         "coalesce(jsonb_agg(" +
            getJsonObjectSelectExpression(fromColumnNames, fromAlias) +
         "),'[]'::jsonb)";
   }
}
