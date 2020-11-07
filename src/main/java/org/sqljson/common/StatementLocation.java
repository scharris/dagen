package org.sqljson.common;

public class StatementLocation
{
   private final String queryName;
   private final String queryPart;

   public StatementLocation(String queryName)
   {
      this.queryName = queryName;
      this.queryPart = "";
   }

   public StatementLocation(String queryName, String queryPart)
   {
      this.queryName = queryName;
      this.queryPart = queryPart;
   }

   public StatementLocation withPart(String additionalPart)
   {
      return new StatementLocation(queryName, joinPartDescriptions(queryPart, additionalPart));
   }

   public String getStatementName() { return queryName; }

   public String getStatementPart() { return queryPart; }

   private static String joinPartDescriptions(String part1, String part2)
   {
      return part1.isEmpty() ? part2 : part1 + " / " + part2;
   }
}
