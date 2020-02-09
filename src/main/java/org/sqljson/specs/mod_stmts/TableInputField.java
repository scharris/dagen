package org.sqljson.specs.mod_stmts;

import java.util.List;
import static java.util.Collections.emptyList;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.fasterxml.jackson.annotation.JsonIgnore;


public class TableInputField
{
   private String field;
   private @Nullable String value = null;
   // Should be non-empty iff generating source and the value is an expression (not simple name).
   private List<String> expressionValueParamNames = emptyList();

   private TableInputField()
   {
      this.field = "";
   }

   public TableInputField
   (
      String field,
      @Nullable String value,
      List<String> expressionValueParamNames
   )
   {
      this.field = field;
      this.value = value;
      this.expressionValueParamNames = expressionValueParamNames;
   }

   public String getField() { return field; }

   public @Nullable String getValue() { return value; }

   public List<String> getExpressionValueParamNames() { return expressionValueParamNames; }

   @JsonIgnore
   public boolean hasSimpleParamValue() { return value == null || !value.endsWith(")"); }
}
