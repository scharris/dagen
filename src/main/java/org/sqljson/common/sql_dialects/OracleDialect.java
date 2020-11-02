package org.sqljson.common.sql_dialects;

import java.util.List;
import java.util.function.Function;

import static java.util.stream.Collectors.joining;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.sqljson.common.specs.FieldParamCondition;
import org.sqljson.mod_stmts.specs.ParametersType;
import org.sqljson.queries.sql.ColumnMetadata;

import static org.sqljson.mod_stmts.specs.ParametersType.NUMBERED;
import static org.sqljson.common.util.Nullables.valueOr;
import static org.sqljson.common.util.StringFuns.*;


public class OracleDialect implements SqlDialect
{
   private final int indentSpaces;

   public OracleDialect(int indentSpaces)
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
         .map(col -> "'" + unDoubleQuote(col.getName()) + "' value " + fromAlias + "." + col.getName())
         .collect(joining(",\n"));

      return
         "json_object(\n" +
            indentLines(objectFieldDecls, indentSpaces) + "\n" +
            "  returning clob\n" +
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
      return "treat(coalesce(json_arrayagg(" + rowObjExpr + " returning clob), to_clob('[]')) as json)";
   }

   @Override
   public String getAggregatedColumnValuesExpression
      (
         ColumnMetadata columnMetadata,
         String fromAlias
      )
   {
      String qfield = fromAlias + "." + columnMetadata.getName();
      return "treat(coalesce(json_arrayagg(" + qfield + " returning clob), to_clob('[]')) as json)";
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

      @Nullable String sql = SqlDialect.getCommonFieldParamConditionSql(mqFieldName, paramValExpr, fpcond.getOpOrDefault());

      if ( sql != null )
         return sql;

      switch ( fpcond.getOpOrDefault() )
      {
         // NOTE: It's intended to eventually add more operators here.
         case JSON_CONTAINS: throw new RuntimeException("Operator not recognized.");
         // (Add other dialect specific operators here.)
         default: throw new RuntimeException("Operator not recognized.");
      }
   }
}

