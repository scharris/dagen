package org.sqljsonquery.sql.dialect;

import java.util.List;


public class OracleDialect implements SqlDialect
{
   private final int indentSpaces;

   public OracleDialect(int indentSpaces)
   {
      this.indentSpaces = indentSpaces;
   }

   public String getJsonObjectSelectExpression
   (
      List<String> fromColumnNames,
      String fromAlias
   )
   {
      throw new RuntimeException("TODO");
   }

   public String getJsonAggregatedObjectsSelectExpression
   (
      List<String> fromColumnNames,
      String fromAlias
   )
   {
      throw new RuntimeException("TODO");
   }
}
