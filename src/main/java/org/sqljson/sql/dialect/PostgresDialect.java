package org.sqljson.sql.dialect;

import java.util.List;
import java.util.function.Function;
import static java.util.stream.Collectors.joining;

import org.checkerframework.checker.nullness.qual.Nullable;

import org.sqljson.specs.FieldParamCondition;
import org.sqljson.specs.mod_stmts.ParametersType;
import org.sqljson.sql.ColumnMetadata;
import org.sqljson.util.StringFuns;
import static org.sqljson.specs.mod_stmts.ParametersType.NUMBERED;
import static org.sqljson.util.Nullables.valueOr;
import static org.sqljson.util.StringFuns.maybeQualify;


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
      return
         "coalesce(jsonb_agg(" +
            getRowObjectExpression(columnMetadatas, fromAlias) +
         "),'[]'::jsonb)";
   }

   @Override
   public String getAggregatedColumnValuesExpression
   (
       ColumnMetadata columnMetadata,
       String fromAlias
   )
   {
      return "coalesce(jsonb_agg(" + fromAlias + "." + columnMetadata.getName() + "))";
   }

   @Override
   public String getAggregatedObjectsFinalQuery(String simpleAggregateQuery, String jsonValueColumnName)
   {
      return simpleAggregateQuery; // no correction necessary
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
