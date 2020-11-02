package org.sqljson.common.specs;

import org.checkerframework.checker.nullness.qual.Nullable;
import com.fasterxml.jackson.annotation.JsonIgnore;


public class FieldParamCondition
{
   public enum Operator {
      EQ, LT, LE, GT, GE, IN, EQ_IF_PARAM_NONNULL, JSON_CONTAINS;

      public boolean acceptsListParam()
      {
         return this.equals(IN);
      }
   }

   private final String field;
   private final @Nullable Operator op;
   private final @Nullable String paramName;

   private FieldParamCondition()
   {
      this.field = "";
      this.op = null;
      this.paramName = null;
   }

   public FieldParamCondition
      (
         String field,
         @Nullable Operator op,
         @Nullable String paramName
      )
   {
      this.field = field;
      this.op = op;
      this.paramName = paramName;
   }

   public String getField() { return field; }

   public @Nullable Operator getOp() { return op; }

   @JsonIgnore
   public Operator getOpOrDefault() { return op != null ? op : Operator.EQ; }

   public @Nullable String getParamName() { return paramName; }
}

