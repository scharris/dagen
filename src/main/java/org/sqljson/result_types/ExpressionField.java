package org.sqljson.result_types;

import java.util.Objects;

import org.checkerframework.checker.nullness.qual.Nullable;


public class ExpressionField
{
   private final String name;
   private final @Nullable String fieldExpression;
   private final @Nullable String typeDeclaration;

   public ExpressionField
      (
         String name,
         @Nullable String fieldExpression,
         @Nullable String typeDeclaration
      )
   {
      this.name = name;
      this.fieldExpression = fieldExpression;
      this.typeDeclaration = typeDeclaration;
   }

   public String getName() { return name; }

   public @Nullable String getFieldExpression() { return fieldExpression; }

   public @Nullable String getTypeDeclaration() { return typeDeclaration; }

   @Override
   public boolean equals(@Nullable Object o)
   {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      ExpressionField that = (ExpressionField) o;
      return
         Objects.equals(fieldExpression, that.fieldExpression) &&
         Objects.equals(name, that.name) &&
         Objects.equals(typeDeclaration, that.typeDeclaration);
   }

   @Override
   public int hashCode()
   {
      return Objects.hash(fieldExpression, name, typeDeclaration);
   }

   @Override
   public String toString()
   {
      return "ExpressionField{" +
         "fieldExpression=" + fieldExpression +
         ", name=" + name +
         ", typeDeclaration=" + typeDeclaration +
         '}';
   }
}

