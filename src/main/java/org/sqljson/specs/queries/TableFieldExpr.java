package org.sqljson.specs.queries;

import static java.util.Objects.requireNonNull;

import org.checkerframework.checker.nullness.qual.Nullable;

import com.fasterxml.jackson.annotation.JsonIgnore;


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

   public String getField() { return requireNonNull(field); }

   public String getExpression() { return requireNonNull(expression); }

   public @Nullable String getWithTableAliasAs() { return withTableAliasAs; }

   public @Nullable String getJsonProperty() { return jsonProperty; }

   public @Nullable String getGeneratedFieldType() { return generatedFieldType; }

   @JsonIgnore
   public boolean isSimpleField()
   {
      if ( (field != null) == (expression != null) )
         throw new RuntimeException("Exactly one of database field name and value expression should be specified.");

      return field != null;
   }
}
