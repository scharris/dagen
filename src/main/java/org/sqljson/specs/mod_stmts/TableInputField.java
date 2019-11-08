package org.sqljson.specs.mod_stmts;

import java.util.Optional;
import static java.util.Optional.empty;


public class TableInputField
{
   private String fieldName;
   private Optional<String> value = empty();

   private TableInputField() {}

   public TableInputField
   (
      String fieldName,
      Optional<String> value
   )
   {
      this.fieldName = fieldName;
      this.value = value;
   }

   public String getFieldName() { return fieldName; }

   public Optional<String> getValue() { return value; }
}
