package org.sqljson.specs.queries;

import java.util.List;
import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;

import org.checkerframework.checker.nullness.qual.Nullable;

import com.fasterxml.jackson.annotation.JsonIgnore;


public final class TableFieldExpr
{
   private @Nullable String field;
   private @Nullable String expression;
   private @Nullable String withTableAliasAs;
   private @Nullable String jsonProperty;
   private List<FieldTypeOverride> generateTypes = emptyList();

   private TableFieldExpr() {}

   public TableFieldExpr
      (
         @Nullable String field,
         @Nullable String expression,
         @Nullable String withTableAliasAs,
         @Nullable String jsonProperty,
         List<FieldTypeOverride> fieldTypeOverrides
      )
   {
      this.field = field;
      this.expression = expression;
      this.withTableAliasAs = withTableAliasAs;
      this.jsonProperty = jsonProperty;
      this.generateTypes = fieldTypeOverrides;

      if ( (field != null) == (expression != null) )
         throw new RuntimeException("Exactly one of database field name and value expression should be specified.");
      if ( withTableAliasAs != null && expression == null )
         throw new RuntimeException("Cannot specify withTableAliasAs without expression value.");
   }

   public String getField() { return requireNonNull(field); }

   public String getExpression() { return requireNonNull(expression); }

   public @Nullable String getWithTableAliasAs() { return withTableAliasAs; }

   public @Nullable String getJsonProperty() { return jsonProperty; }

   public List<FieldTypeOverride> getGenerateTypes() { return generateTypes; }

   public @Nullable FieldTypeOverride getTypeOverride(String language)
   {
      return generateTypes.stream().filter(to -> to.getLanguage().equals(language)).findAny().orElse(null);
   }

   @JsonIgnore
   public boolean isSimpleField()
   {
      if ( (field != null) == (expression != null) )
         throw new RuntimeException("Exactly one of database field name and value expression should be specified.");

      return field != null;
   }
}

