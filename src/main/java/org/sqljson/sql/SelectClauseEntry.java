package org.sqljson.sql;

import org.checkerframework.checker.nullness.qual.Nullable;


public class SelectClauseEntry
{
   public enum Source { NATIVE_FIELD, INLINE_PARENT, PARENT_REFERENCE, CHILD_COLLECTION, HIDDEN_PK }

   private final String valueExpression;
   private final String outputName;
   private final Source source;
   private final @Nullable String generatedFieldType;

   public SelectClauseEntry
      (
         String valueExpression,
         String outputName,
         Source source
      )
   {
      this(valueExpression, outputName, source, null);
   }

   public SelectClauseEntry
      (
         String valueExpression,
         String outputName,
         Source source,
         @Nullable String generatedFieldType
      )
   {
      this.valueExpression = valueExpression;
      this.outputName = outputName;
      this.source = source;
      this.generatedFieldType = generatedFieldType;
   }

   public String getValueExpression() { return valueExpression; }

   public String getName() { return outputName; }

   public Source getSource() { return source; }

   public @Nullable String getGeneratedFieldType() { return generatedFieldType; }
}

