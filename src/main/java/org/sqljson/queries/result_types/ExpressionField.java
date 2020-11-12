package org.sqljson.queries.result_types;

import java.util.Objects;

import org.checkerframework.checker.nullness.qual.Nullable;


public class ExpressionField
{
   private final String name;
   private final @Nullable String fieldExpression;
   private final @Nullable String specifiedSourceCodeFieldType;

   public ExpressionField
      (
         String name,
         @Nullable String fieldExpression,
         @Nullable String specifiedSourceCodeFieldType
      )
   {
      this.name = name;
      this.fieldExpression = fieldExpression;
      this.specifiedSourceCodeFieldType = specifiedSourceCodeFieldType;
   }

   public String getName() { return name; }

   public @Nullable String getFieldExpression() { return fieldExpression; }

   public @Nullable String getSpecifiedSourceCodeFieldType() { return specifiedSourceCodeFieldType; }

   @Override
   public boolean equals(@Nullable Object o)
   {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      ExpressionField that = (ExpressionField) o;
      return
         Objects.equals(fieldExpression, that.fieldExpression) &&
         Objects.equals(name, that.name) &&
         Objects.equals(specifiedSourceCodeFieldType, that.specifiedSourceCodeFieldType);
   }

   @Override
   public int hashCode()
   {
      return Objects.hash(fieldExpression, name, specifiedSourceCodeFieldType);
   }

   @Override
   public String toString()
   {
      return "ExpressionField{" +
         "fieldExpression=" + fieldExpression +
         ", name=" + name +
         ", specifiedSourceCodeFieldType=" + specifiedSourceCodeFieldType +
         '}';
   }
}

