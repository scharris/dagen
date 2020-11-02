package org.sqljson.queries.specs;

import org.checkerframework.checker.nullness.qual.Nullable;


public final class TableFieldExpr
{
   private @Nullable String field;
   private @Nullable String expression;
   private @Nullable String withTableAliasAs;
   private @Nullable String jsonProperty;
   private @Nullable String generatedFieldType;

   private TableFieldExpr() {}

   public TableFieldExpr
      (
         @Nullable String field,
         @Nullable String expression,
         @Nullable String withTableAliasAs,
         @Nullable String jsonProperty,
         @Nullable String generatedFieldType
      )
   {
      this.field = field;
      this.expression = expression;
      this.withTableAliasAs = withTableAliasAs;
      this.jsonProperty = jsonProperty;
      this.generatedFieldType = generatedFieldType;

      if ( (field != null) == (expression != null) )
         throw new RuntimeException("Exactly one of database field name and value expression should be specified.");
      if ( withTableAliasAs != null && expression == null )
         throw new RuntimeException("Cannot specify withTableAliasAs without expression value.");
   }

   public @Nullable String getField() { return field; }

   public @Nullable String getExpression() { return expression; }

   public @Nullable String getWithTableAliasAs() { return withTableAliasAs; }

   public @Nullable String getJsonProperty() { return jsonProperty; }

   public @Nullable String getGeneratedFieldType() { return generatedFieldType; }
}
