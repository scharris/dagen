package org.sqljson.specs.mod_stmts;

import java.util.Optional;
import static java.util.Optional.empty;


public class FieldParamEquality
{
   private String fieldName;
   private Optional<String> paramName = empty();

   private FieldParamEquality() {}

   public FieldParamEquality(String fieldName, Optional<String> paramName)
   {
      this.fieldName = fieldName;
      this.paramName = paramName;
   }

   public String getFieldName() { return fieldName; }

   public Optional<String> getParamName() { return paramName; }
}
