package org.sqljson.common;


public class StatementSpecificationException extends RuntimeException
{
   private final String statementsSource;
   private final String statementName;
   private final String statementPart;
   private final String problem;

   public StatementSpecificationException
      (
         String statementsSource,
         String statementName,
         String statementPart,
         String problem
      )
    {
       super("In specification \"" + statementsSource + "\" in statement \"" + statementName + "\" " +
          "at " + statementPart + ": " + problem);

       this.statementsSource = statementsSource;
       this.statementName = statementName;
       this.statementPart = statementPart;
       this.problem = problem;
    }

   public String getStatementsSource() { return statementsSource; }

   public String getStatementName() { return statementName; }

   public String getStatementPart() { return statementPart; }

   public String getProblem() { return problem; }
}
