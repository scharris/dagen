package org.sqljson.sql.dialect;

import java.util.List;
import java.util.function.Function;

import static java.util.stream.Collectors.joining;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.sqljson.specs.FieldParamCondition;
import org.sqljson.specs.mod_stmts.ParametersType;
import org.sqljson.sql.ColumnMetadata;

import static org.sqljson.specs.mod_stmts.ParametersType.NUMBERED;
import static org.sqljson.sql.SelectClauseEntry.Source.CHILD_COLLECTION;
import static org.sqljson.util.Nullables.valueOr;
import static org.sqljson.util.StringFuns.*;


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
         .map(col -> "'" + unDoubleQuote(col.getName()) + "' value " +
            (col.getSource() == CHILD_COLLECTION ?
               "treat(" + fromAlias + "." + col.getName() + " as json)"
               :  fromAlias + "." + col.getName())
         )
         .collect(joining(",\n"));

      return
         "json_object(\n" +
            indentLines(objectFieldDecls, indentSpaces) + "\n" +
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
         "json_arrayagg(" +
            getRowObjectExpression(columnMetadatas, fromAlias) +
         " returning clob)";
   }

   @Override
   public String getAggregatedColumnValuesExpression
      (
         ColumnMetadata columnMetadata,
         String fromAlias
      )
   {
      String qfield = fromAlias + "." + columnMetadata.getName();
      return "coalesce(json_arrayagg(" + qfield + " returning clob), to_clob('[]'))";
   }

   /// Replace empty clob returned by json_arrayagg() when aggregating over no
   /// source rows with empty json array.
   /* NOTE
   Oracle unfortunately has json_arrayagg() return an empty clob when aggregating
   over no rows, whereas we need an empty json array value in that case. Since an
   empty clob is non-null, it's not easy to replace at the level of the aggregate
   select expression, without introducing a new function in the schema which is
   to be avoided. So we resort to wrapping the aggregate query in another query
   here, which can replace the faulty empty aggregate representation.
   */
   @Override
   public String getAggregatedObjectsFinalQuery(String simpleAggregatedObjectsQuery, String jsonValueColumnName)
   {
      String origAggVal = "q." + jsonValueColumnName;

      return
         "select case when dbms_lob.getlength(" + origAggVal + ") = 0 then to_clob('[]') " +
         "else " + origAggVal + " end " +
         "from (\n" +
            indentLines(simpleAggregatedObjectsQuery, 2) + "\n" +
         ") q";
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
      else
         throw new RuntimeException("Sql dialect does not support operator " + fpcond.getOp() + ".");
   }
}

