package org.sqljson.specs.queries;

import java.util.List;
import java.util.Optional;
import static java.util.Collections.emptyList;

import com.fasterxml.jackson.annotation.JsonIgnore;

import org.sqljson.util.StringFuns;


public final class TableFieldExpr
{
   private Optional<String> field = Optional.empty();
   private Optional<String> expression = Optional.empty();
   private Optional<String> jsonProperty = Optional.empty();
   private List<FieldTypeOverride> generateTypes = emptyList();

   private TableFieldExpr() {}

   public TableFieldExpr
   (
      Optional<String> field,
      Optional<String> expression,
      Optional<String> jsonProperty,
      List<FieldTypeOverride> fieldTypeOverrides
   )
   {
      this.field = field;
      this.expression = expression;
      this.jsonProperty = jsonProperty;
      this.generateTypes = fieldTypeOverrides;

      if ( field.isPresent() == expression.isPresent() )
         throw new RuntimeException("Exactly one of database field name and value expression should be specified.");
   }

   public String getField() { return field.get(); }

   public String getExpression() { return expression.get(); }

   public Optional<String> getJsonProperty() { return jsonProperty; }

   public List<FieldTypeOverride> getGenerateTypes() { return generateTypes; }

   public Optional<FieldTypeOverride> getTypeOverride(String language)
   {
      return generateTypes.stream().filter(to -> to.getLanguage().equals(language)).findAny();
   }

   @JsonIgnore
   public boolean isSimpleField()
   {
      if ( field.isPresent() == expression.isPresent() )
         throw new RuntimeException("Exactly one of database field name and value expression should be specified.");

      return field.isPresent();
   }

   public String getValueExpressionForAlias(String tableAlias)
   {
      if ( isSimpleField() )
         return tableAlias + "." + field.get();
      else
         return StringFuns.substituteVarValue(expression.get(), tableAlias);
   }
}
