package org.sqljson.sql_dialects;

import java.util.List;
import java.util.function.Function;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.sqljson.dbmd.DatabaseMetadata;
import org.sqljson.specs_common.FieldParamCondition;
import org.sqljson.mod_stmts.specs.ParametersType;
import org.sqljson.queries.sql.ColumnMetadata;


public interface SqlDialect
{
   enum DbmsType { PG, ORA, ISO }

   String getRowObjectExpression
      (
         List<ColumnMetadata> columnMetadatas,
         String fromAlias
      );

   /// Select expression part of a simple aggregate objects query. This expression should be an aggregate function
   /// which builds an array of json objects from a source relation having the given column names and table alias.
   String getAggregatedRowObjectsExpression
      (
         List<ColumnMetadata> columnMetadatas,
         String fromAlias
      );

   String getAggregatedColumnValuesExpression
      (
         ColumnMetadata columnMetadata,
         String fromAlias
      );

   String getFieldParamConditionSql
      (
         FieldParamCondition fieldParamCondition,
         @Nullable String tableAlias,
         ParametersType paramsType,
         Function<String,String> defaultParamNameFn // default param name as function of field name
      );

   static SqlDialect fromDatabaseMetadata
      (
         DatabaseMetadata dbmd,
         int indentSpaces
      )
   {
      DbmsType dbmsType = getDbmsType(dbmd.getDbmsName());
      switch ( dbmsType )
      {
         case PG: return new PostgresDialect(indentSpaces);
         case ORA: return new OracleDialect(indentSpaces);
         default: throw new RuntimeException("dbms type " + dbmsType + " is currently not supported");
      }
   }

   static DbmsType getDbmsType(String dbmsName)
   {
      String dbmsLower = dbmsName.toLowerCase();
      if ( dbmsLower.contains("postgres") ) return DbmsType.PG;
      else if ( dbmsLower.contains("oracle") ) return DbmsType.ORA;
      else return DbmsType.ISO;
   }

   static @Nullable String getCommonFieldParamConditionSql
      (
         String mqFieldName, // field name, maybe qualified
         String paramValExpr,
         FieldParamCondition.Operator op
      )
   {
      switch ( op )
      {
         case EQ: return mqFieldName + " = " + paramValExpr;
         case LT: return mqFieldName + " < " + paramValExpr;
         case LE: return mqFieldName + " <= " + paramValExpr;
         case GT: return mqFieldName + " > " + paramValExpr;
         case GE: return mqFieldName + " >= " + paramValExpr;
         case IN: return mqFieldName + " IN (" + paramValExpr + ")";
         case EQ_IF_PARAM_NONNULL: return "(" + paramValExpr + " is null or " + mqFieldName + " = " + paramValExpr + ")";
         default: return null;
      }
   }
}

