package org.sqljson.specs.mod_stmts;

import java.util.Optional;
import static java.util.Optional.empty;


public class FieldParamCondition
{
   public enum Operator { EQ, LT, LE, GT, GE, IN, EQ_ANY }

   private String fieldName;
   private Operator op = Operator.EQ;
   private Optional<String> paramName = empty();

   private FieldParamCondition() {}

   public FieldParamCondition(String fieldName, Operator op, Optional<String> paramName)
   {
      this.fieldName = fieldName;
      this.op = op;
      this.paramName = paramName;
   }

   public String getFieldName() { return fieldName; }

   public Operator getOp() { return op; }

   public Optional<String> getParamName() { return paramName; }
}
