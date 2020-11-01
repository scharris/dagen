package org.sqljson.common.sql_dialects;

import java.util.List;
import java.util.function.Function;
import static java.util.stream.Collectors.joining;

import org.checkerframework.checker.nullness.qual.Nullable;

import org.sqljson.common.specs.FieldParamCondition;
import org.sqljson.mod_stmts.specs.ParametersType;
import org.sqljson.queries.sql.ColumnMetadata;
import org.sqljson.common.util.StringFuns;
import static org.sqljson.mod_stmts.specs.ParametersType.NUMBERED;
import static org.sqljson.common.util.Nullables.valueOr;
import static org.sqljson.common.util.StringFuns.maybeQualify;


public class PostgresDialect implements SqlDialect
{
   private final int indentSpaces;

   public PostgresDialect(int indentSpaces)
   {
      this.indentSpaces = indentSpaces;
   }

   @Override
   public String getRowObjectExpression
      (
         List<ColumnMetadata> columnMetadatas,
         String fromAlias
      )
   {
      String objectFieldDecls =
         columnMetadatas.stream()
         .map(col -> "'" + StringFuns.unDoubleQuote(col.getName()) + "', " + fromAlias + "." + col.getName())
         .collect(joining(",\n"));

         return
            "jsonb_build_object(\n" +
               StringFuns.indentLines(objectFieldDecls, indentSpaces) + "\n" +
            ")";
   }

   @Override
   public String getAggregatedRowObjectsExpression
      (
         List<ColumnMetadata> columnMetadatas,
         String fromAlias
      )
   {

      String rowObjExpr = getRowObjectExpression(columnMetadatas, fromAlias);
      return "coalesce(jsonb_agg(" + rowObjExpr + "),'[]'::jsonb)";
   }

   @Override
   public String getAggregatedColumnValuesExpression
      (
          ColumnMetadata columnMetadata,
          String fromAlias
      )
   {
      String qfield = fromAlias + "." + columnMetadata.getName();
      return "coalesce(jsonb_agg(" + qfield + "))";
   }

   @Override
   public String getFieldParamConditionSql
      (
         FieldParamCondition fpcond,
         @Nullable String tableAlias,
         ParametersType paramsType,
         Function<String,String> defaultParamNameFn // default param name as function of field name
      )
   {
      String mqFieldName = maybeQualify(tableAlias, fpcond.getField());
      String paramValExpr = paramsType == NUMBERED ? "?" : ":"+ valueOr(fpcond.getParamName(), defaultParamNameFn.apply(fpcond.getField()));

      @Nullable String sql = SqlDialect.getCommonFieldParamConditionSql(mqFieldName, paramValExpr, fpcond.getOp());

      if ( sql != null )
         return sql;

      switch ( fpcond.getOp() )
      {
         case JSON_CONTAINS: return mqFieldName + " @> " + paramValExpr;
         // (Add other dialect specific operators here.)
         default: throw new RuntimeException("Operator not recognized.");
      }
   }
}

