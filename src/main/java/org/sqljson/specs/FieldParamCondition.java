package org.sqljson.specs;

import java.util.function.Function;
import org.checkerframework.checker.nullness.qual.Nullable;

import org.sqljson.specs.mod_stmts.ParametersType;
import static org.sqljson.specs.mod_stmts.ParametersType.NUMBERED;
import static org.sqljson.util.Nullables.valueOr;
import static org.sqljson.util.StringFuns.maybeQualify;


public class FieldParamCondition
{
   public enum Operator {
      EQ, LT, LE, GT, GE, IN, EQ_IF_PARAM_NONNULL;

      public boolean acceptsListParam()
      {
         return this.equals(IN);
      }
   }

   private String field;
   private Operator op = Operator.EQ;
   private @Nullable String paramName = null;

   private FieldParamCondition()
   {
      this.field = "";
   }

   public FieldParamCondition(String field, Operator op, @Nullable String paramName)
   {
      this.field = field;
      this.op = op;
      this.paramName = paramName;
   }

   public String getField() { return field; }

   public Operator getOp() { return op; }

   public @Nullable String getParamName() { return paramName; }

   public String getFinalParamName(Function<String,String> defaultParamNameFn) // default param name as function of field name
   {
      return valueOr(paramName, defaultParamNameFn.apply(field));
   }

   public String toSql
   (
      @Nullable String tableAlias,
      ParametersType paramsType,
      Function<String,String> defaultParamNameFn // default param name as function of field name
   )
   {
      String mqFieldName = maybeQualify(tableAlias, field);
      String paramValExpr = paramsType == NUMBERED ? "?" : ":"+getFinalParamName(defaultParamNameFn);

      switch ( getOp() )
      {
         case EQ: return mqFieldName + " = " + paramValExpr;
         case LT: return mqFieldName + " < " + paramValExpr;
         case LE: return mqFieldName + " <= " + paramValExpr;
         case GT: return mqFieldName + " > " + paramValExpr;
         case GE: return mqFieldName + " >= " + paramValExpr;
         case IN: return mqFieldName + " IN (" + paramValExpr + ")";
         case EQ_IF_PARAM_NONNULL: return "(" + paramValExpr + " is null or " + mqFieldName + " = " + paramValExpr + ")";
         default: throw new RuntimeException("Operator not recognized.");
      }
   }
}
