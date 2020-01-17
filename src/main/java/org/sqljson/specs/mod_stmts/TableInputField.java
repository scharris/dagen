package org.sqljson.specs.mod_stmts;

import java.util.List;
import java.util.Optional;
import static java.util.Collections.emptyList;
import static java.util.Optional.empty;

import com.fasterxml.jackson.annotation.JsonIgnore;


public class TableInputField
{
   private String field;
   private Optional<String> value = empty();
   // Should be non-empty iff generating source and the value is an expression (not simple name).
   private List<String> expressionValueParamNames = emptyList();

   private TableInputField() {}

   public TableInputField
   (
      String field,
      Optional<String> value,
      List<String> expressionValueParamNames
   )
   {
      this.field = field;
      this.value = value;
      this.expressionValueParamNames = expressionValueParamNames;
   }

   public String getField() { return field; }

   public Optional<String> getValue() { return value; }

   public List<String> getExpressionValueParamNames() { return expressionValueParamNames; }

   @JsonIgnore
   public boolean hasSimpleParamValue() { return !value.isPresent() || !value.get().endsWith(")"); }
}
