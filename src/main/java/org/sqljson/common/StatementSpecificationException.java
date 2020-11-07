package org.sqljson.common;


public class StatementSpecificationException extends RuntimeException
{
   private final String statementsSource;
   private final StatementLocation statementLocation;
   private final String problem;

   public StatementSpecificationException
      (
         String statementsSource,
         StatementLocation statementLocation,
         String problem
      )
    {
       super("In specification \"" + statementsSource + "\" in statement \"" +
          statementLocation.getStatementName() + "\" " +
          "at " + statementLocation.getStatementPart() + ": " + problem);

       this.statementsSource = statementsSource;
       this.statementLocation = statementLocation;
       this.problem = problem;
    }

   public String getStatementsSource() { return statementsSource; }

   public StatementLocation getStatementLocation() { return statementLocation; }

   public String getProblem() { return problem; }
}
