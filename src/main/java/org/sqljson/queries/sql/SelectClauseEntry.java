package org.sqljson.queries.sql;

import org.checkerframework.checker.nullness.qual.Nullable;


public class SelectClauseEntry
{
   public enum Source { NATIVE_FIELD, INLINE_PARENT, PARENT_REFERENCE, CHILD_COLLECTION, HIDDEN_PK }

   private final String valueExpression;
   private final String outputName;
   private final Source source;
   private final @Nullable String comment;

   public SelectClauseEntry
      (
         String valueExpression,
         String outputName,
         Source source,
         @Nullable String comment
      )
   {
      this.valueExpression = valueExpression;
      this.outputName = outputName;
      this.source = source;
      this.comment = comment;
   }

   public String getValueExpression() { return valueExpression; }

   public String getName() { return outputName; }

   public Source getSource() { return source; }

   public @Nullable String getComment() { return comment; }
}

