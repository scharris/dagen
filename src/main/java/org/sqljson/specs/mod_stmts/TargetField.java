package org.sqljson.specs.mod_stmts;

import java.util.List;
import static java.util.Collections.emptyList;


public class TargetField
{
   private String field;
   private String value;
   private List<String> paramNames = emptyList();

   private TargetField()
   {
      this.field = "";
      this.value = "";
   }

   public TargetField
      (
         String field,
         String value,
         List<String> paramNames
      )
   {
      this.field = field;
      this.value = value;
      this.paramNames = paramNames;
   }

   public String getField() { return field; }

   public String getValue() { return value; }

   public List<String> getParamNames() { return paramNames; }
}
