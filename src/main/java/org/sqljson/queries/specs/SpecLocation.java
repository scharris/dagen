package org.sqljson.queries.specs;


public class SpecLocation
{
   private final String queryName;
   private final String queryPart;

   public SpecLocation(String queryName)
   {
      this.queryName = queryName;
      this.queryPart = "";
   }

   public SpecLocation(String queryName, String queryPart)
   {
      this.queryName = queryName;
      this.queryPart = queryPart;
   }

   public SpecLocation addPart(String additionalPart)
   {
      return new SpecLocation(queryName, joinPartDescriptions(queryPart, additionalPart));
   }

   public String getStatementName() { return queryName; }

   public String getStatementPart() { return queryPart; }

   private static String joinPartDescriptions(String part1, String part2)
   {
      return part1.isEmpty() ? part2 : part1 + " / " + part2;
   }
}
