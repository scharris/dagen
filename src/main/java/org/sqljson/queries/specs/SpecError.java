package org.sqljson.queries.specs;


public class SpecError extends RuntimeException
{
   private final SpecLocation specLocation;
   private final String problem;

   public SpecError
      (
         SpecLocation specLocation,
         String problem
      )
    {
       super("In statement \"" + specLocation.getQueryName() + "\" " +
             "at " + specLocation.getQueryPart() + ": " + problem);

       this.specLocation = specLocation;
       this.problem = problem;
    }

   public SpecLocation getSpecLocation() { return specLocation; }

   public String getProblem() { return problem; }

   public static SpecError specError
      (
         QuerySpec querySpec,
         String queryPart,
         String problem
      )
   {
      return new SpecError(new SpecLocation(querySpec.getQueryName(), queryPart), problem);
   }
}
