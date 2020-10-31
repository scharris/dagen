package org.sqljson.queries.sql;


public class SelectClauseEntry
{
   public enum Source { NATIVE_FIELD, INLINE_PARENT, PARENT_REFERENCE, CHILD_COLLECTION, HIDDEN_PK }

   private final String valueExpression;
   private final String outputName;
   private final Source source;

   public SelectClauseEntry
      (
         String valueExpression,
         String outputName,
         Source source
      )
   {
      this.valueExpression = valueExpression;
      this.outputName = outputName;
      this.source = source;
   }

   public String getValueExpression() { return valueExpression; }

   public String getName() { return outputName; }

   public Source getSource() { return source; }
}

