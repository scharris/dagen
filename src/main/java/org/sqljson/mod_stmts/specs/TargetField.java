package org.sqljson.mod_stmts.specs;

import java.util.List;
import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;

import org.checkerframework.checker.nullness.qual.Nullable;
import com.fasterxml.jackson.annotation.JsonIgnore;


public class TargetField
{
   private String field;
   private String value;
   private @Nullable List<String> paramNames = null;

   private TargetField()
   {
      this.field = "";
      this.value = "";
      this.paramNames = null;
   }

   public TargetField
      (
         String field,
         String value,
         @Nullable List<String> paramNames
      )
   {
      this.field = field;
      this.value = value;
      this.paramNames = paramNames != null ? unmodifiableList(paramNames) : null;
   }

   public String getField() { return field; }

   public String getValue() { return value; }

   public @Nullable List<String> getParamNames() { return paramNames; }

   @JsonIgnore
   public List<String> getParamNamesList()
   {
      return paramNames != null ? paramNames : emptyList();
   }
}
