package org.sqljson.queries;

import java.util.*;
import java.util.function.Function;
import static java.util.Collections.*;
import static java.util.stream.Collectors.*;
import static java.util.function.Function.identity;

import org.checkerframework.checker.nullness.qual.Nullable;

import org.sqljson.specs_common.StatementSpecificationException;
import org.sqljson.dbmd.*;
import org.sqljson.queries.result_types.GeneratedType;
import org.sqljson.queries.specs.*;
import org.sqljson.specs_common.FieldParamCondition;
import org.sqljson.specs_common.RecordCondition;
import org.sqljson.queries.sql.*;
import org.sqljson.util.StringFuns;
import org.sqljson.sql_dialects.SqlDialect;

import static org.sqljson.util.StatementValidations.identifySpecificationTable;
import static org.sqljson.util.StatementValidations.verifyTableFieldsExist;
import static org.sqljson.util.Nullables.*;
import static org.sqljson.util.StringFuns.*;
import static org.sqljson.dbmd.ForeignKeyScope.REGISTERED_TABLES_ONLY;
import static org.sqljson.mod_stmts.specs.ParametersType.NAMED;


public class QueryGenerator
{
   private final DatabaseMetadata dbmd;
   private final SqlDialect sqlDialect;
   private final @Nullable String defaultSchema;
   private final Set<String> generateUnqualifiedNamesForSchemas;
   private final int indentSpaces;
   private final QueryTypesGenerator queryTypesGenerator;
   private final Function<String,String> defaultPropNameFn; // default output property naming function
   private final String queriesSource;

   private static final String HIDDEN_PK_PREFIX = "_";

   private static final String DEFAULT_TABLE_ALIAS_VAR = "$$";

   /*
   Note: field name quoting
   - A field name from the database metadata (DBMD) is quoted iff its interpretation by the database would change
     with quoting. So DBMD.quoteIfNeeded is applied to all field names from the database metadata when used in queries.
   - Output field names in the queries spec file (QueryGroup contents) always have DBMD.quoteIfNeeded applied before
     use to preserve letter case.
   - Database field names in the queries spec file are always used as-is in queries without quoting or case conversion.
     Users must quote these identifiers within the specifications file where they are not usable in queries directly,
     such as if they contain non-alphabetic characters or match a SQL keyword. For most schemas none of these field
     references would need quoting and the spec files can safely be used across database types.
   Summary: DBMD.quoteIfNeeded is applied to all field names except database field names from the queries spec file.
   */

   public QueryGenerator
      (
         DatabaseMetadata dbmd,
         @Nullable String defaultSchema,
         Set<String> generateUnqualifiedNamesForSchemas,
         Function<String,String> defaultPropNameFn,
         String queriesSource
      )
   {
      this.dbmd = dbmd;
      this.indentSpaces = 2;
      this.sqlDialect = SqlDialect.fromDatabaseMetadata(this.dbmd, this.indentSpaces);
      this.defaultSchema = defaultSchema;
      this.generateUnqualifiedNamesForSchemas = generateUnqualifiedNamesForSchemas.stream().map(dbmd::normalizeName).collect(toSet());
      this.defaultPropNameFn = defaultPropNameFn;
      this.queriesSource = queriesSource;
      this.queryTypesGenerator = new QueryTypesGenerator(dbmd, defaultSchema, defaultPropNameFn);
   }

   public GeneratedQuery generateQuery
      (
         QuerySpec querySpec
      )
   {
      String queryName = querySpec.getQueryName();

      // This query spec may customize the default output field name making function.
      Function<String,String> propNameDefaultFn =
         applyOr(querySpec.getOutputFieldNameDefault(), OutputFieldNameDefault::toFunctionOfFieldName,
                 this.defaultPropNameFn);

      Map<ResultsRepr,String> querySqls =
         querySpec.getResultsRepresentations().stream()
         .collect(toMap(identity(), repr ->
            makeSqlForResultsRepr(querySpec, repr, propNameDefaultFn)
         ));

      List<GeneratedType> generatedTypes = querySpec.getGenerateResultTypes() ?
            queryTypesGenerator.generateTypes(querySpec.getTableJson(), emptyMap())
            : emptyList();

      return new
         GeneratedQuery(
            queryName,
            querySqls,
            generatedTypes,
            querySpec.getGenerateSource(),
            querySpec.getTypesFileHeader(),
            getAllParamNames(querySpec)
      );
   }

   private String makeSqlForResultsRepr
      (
         QuerySpec querySpec,
         ResultsRepr resultsRepr,
         Function<String,String> propNameDefaultFn
      )
   {
      TableJsonSpec tjs = querySpec.getTableJson();
      String queryName = querySpec.getQueryName();

      switch ( resultsRepr )
      {
         case MULTI_COLUMN_ROWS:
         {
            String q = makeBaseQuery(tjs, null, false, propNameDefaultFn, queryName, "" )
                       .getSql();
            return querySpec.getForUpdate() ? q + "\nfor update" : q;
         }
         case JSON_OBJECT_ROWS:
         {
            if ( querySpec.getForUpdate() )
               throw specError(
                  querySpec.getQueryName(),
                  "for update clause",
                  "FOR UPDATE queries are only allowed for MULTI_COLUMN_ROWS results"
               );

            return makeJsonObjectRowsSql( tjs, null, propNameDefaultFn, queryName, "");
         }
         case JSON_ARRAY_ROW:
         {
            if ( querySpec.getForUpdate() )
               throw specError(
                  querySpec.getQueryName(),
                  "for update clause",
                  "FOR UPDATE queries are only allowed for MULTI_COLUMN_ROWS results"
               );

            return makeAggregatedJsonResultSql(tjs, null, propNameDefaultFn, false, queryName, "");
         }
         default:
            throw specError(
               querySpec.getQueryName(),
               "resultsRepresentations",
               "Results representation '" + resultsRepr + "' is not valid."
            );
      }
   }

   /** Generate SQL and column name metadata for the given table output
    *  specification and parent/child condition, with multi-column and multi-row
    *  representation of results.
    * @param tableJsonSpec
    *    The output specification for this table, the subject of the query.
    * @param parentChildCond
    *    A filter condition on this table from a parent or child table whose
    *    alias (accessible from the condition) can be assumed to be in context.
    * @param exportPkFieldsHidden
    *    If enabled then all primary key fields will be added to the SQL select
    *    clause but not are not listed in the result columns list which
    *    is reserved for columns intended for final results. The primary key
    *    columns added for this option have prefixed output names to avoid name
    *    collisions. This is useful for filtering results of this base query
    *    such as for parent child relationship conditions.
    * @return
    *    A BaseQuery structure containing the generated SQL and some metadata
    *    about the query (e.g. column names).
    */
   private BaseQuery makeBaseQuery
      (
         TableJsonSpec tableJsonSpec,
         @Nullable ParentChildCondition parentChildCond,
         boolean exportPkFieldsHidden,
         Function<String,String> propNameDefaultFn,
         String queryName, // for error details
         String queryPart  // "
      )
   {
      SqlQueryParts q = new SqlQueryParts();

      RelId relId = identifyTable(tableJsonSpec.getTable(), queryName, queryPart);

      ifPresent(parentChildCond, pcCond ->
         q.addAliasToScope(pcCond.getOtherTableAlias())
      );

      String alias = q.makeNewAliasFor(relId.getName());
      q.addFromClauseEntry(minimalRelIdentifier(relId) + " " + alias);

      // If exporting pk fields, add any that aren't already in the output fields list to the select clause.
      if ( exportPkFieldsHidden )
         for ( String pkFieldName : dbmd.getPrimaryKeyFieldNames(relId) )
         {
            String pkFieldDbName = dbmd.quoteIfNeeded(pkFieldName);
            String pkFieldOutputName = dbmd.quoteIfNeeded(HIDDEN_PK_PREFIX + pkFieldName);
            q.addSelectClauseEntry(
               alias + "." + pkFieldDbName,
               pkFieldOutputName,
               SelectClauseEntry.Source.HIDDEN_PK
            );
         }

      // Add this table's own field expressions to the select clause.
      verifySimpleSelectFieldsExist(tableJsonSpec, queryName, queryPart);
      //
      for ( TableFieldExpr tfe : tableJsonSpec.getFieldExpressions() )
         q.addSelectClauseEntry(
            getTableFieldExpressionForAlias(tfe, alias),
            dbmd.quoteIfNeeded(getJsonPropertyName(tfe, propNameDefaultFn, tableJsonSpec.getTable())),
            SelectClauseEntry.Source.NATIVE_FIELD
         );

      // Add child record collections to the select clause.
      for ( ChildCollectionSpec childSpec : tableJsonSpec.getChildTableCollections() )
      {
         String childPart =  joinPartDescriptions(queryPart, "child collection '" + childSpec.getCollectionName() + "'");
         String childQuery = makeChildRecordsQuery(childSpec, relId, alias, propNameDefaultFn, queryName, childPart);
         q.addSelectClauseEntry(
            "(\n" + indent(childQuery) + "\n)",
            dbmd.quoteIfNeeded(childSpec.getCollectionName()),
            SelectClauseEntry.Source.CHILD_COLLECTION
         );
      }

      // Add query parts for inline parents.
      for ( InlineParentSpec parentSpec : tableJsonSpec.getInlineParentTables() )
      {
         String parentPart = joinPartDescriptions(queryPart, "inline parent '" + parentSpec.getTableJson().getTable() + "'");
         Set<String> aliases = q.getAliasesInScope();
         q.addParts(
            getInlineParentBaseQueryParts(parentSpec, relId, alias, aliases, propNameDefaultFn, queryName, parentPart)
         );
      }

      // Add parts for referenced parents.
      for ( ReferencedParentSpec parentSpec : tableJsonSpec.getReferencedParentTables() )
      {
         String parentPart = joinPartDescriptions(queryPart, "parent '" + parentSpec.getTableJson().getTable() + "'");
         q.addParts(
            getReferencedParentBaseQueryParts(parentSpec, relId, alias, propNameDefaultFn, queryName, parentPart)
         );
      }

      // Add parent/child relationship filter condition if any to the where clause.
      ifPresent(parentChildCond, pcCond ->
         q.addWhereClauseEntry(pcCond.asEquationConditionOn(alias, dbmd))
      );

      @Nullable String whereCond =
         getWhereCondition(tableJsonSpec, alias, queryName, joinPartDescriptions(queryPart, "where clause"));
      ifPresent(whereCond, q::addWhereClauseEntry);

      String sql = makeSqlFromParts(q);

      List<ColumnMetadata> columnMetadatas =
         q.getSelectClauseEntries().stream()
         .filter(e -> e.getSource() != SelectClauseEntry.Source.HIDDEN_PK)
         .map(e -> new ColumnMetadata(e.getName()))
         .collect(toList());

      return new BaseQuery(sql, columnMetadatas);
   }

   /** Make a query having a single row and column result, with the result value
    *  representing the collection of json object representations of all rows
    *  of the table whose output specification is passed.
    * @param tjs  The output specification for this table, the subject of the query.
    * @param parentChildCond A filter condition on this table (always) from a parent or child table whose alias
    *                        (accessible from the condition) can be assumed to be in context.
    * @return the generated SQL query
    */
   private String makeAggregatedJsonResultSql
      (
         TableJsonSpec tjs,
         @Nullable ParentChildCondition parentChildCond,
         Function<String,String> propNameDefaultFn,
         boolean unwrapSingleColumnValues,
         String queryName,
         String queryPart
      )
   {
      BaseQuery baseQuery =
         makeBaseQuery(tjs, parentChildCond, false, propNameDefaultFn, queryName, queryPart);

      if ( unwrapSingleColumnValues && baseQuery.getResultColumnMetadatas().size() != 1 )
         throw specError(
            queryName,
            queryPart,
            "Collection of " + tjs.getTable() + " rows cannot have " +
             "unwrapped option specified where multiple field expressions are included."
         );

      String aggExpr = unwrapSingleColumnValues ?
          sqlDialect.getAggregatedColumnValuesExpression(baseQuery.getResultColumnMetadatas().get(0), "q")
          : sqlDialect.getAggregatedRowObjectsExpression(baseQuery.getResultColumnMetadatas(), "q");

      return
         "select\n" +
            indent(aggExpr) + " json\n" +
         "from (\n" +
            indent(baseQuery.getSql()) + "\n" +
         ") q";
   }

   /** Make a query having JSON object result values at the top level of the
    *  result set. The query returns a JSON value in a single column and with
    *  any number of result rows.
    * @param tjs  The output specification for this table, the subject of the query.
    * @param parentChildCond A filter condition on this table (always) from a parent or child table whose alias
    *                        (accessible from the condition) can be assumed to be in context.
    * @return the generated SQL query
    */
   private String makeJsonObjectRowsSql
      (
         TableJsonSpec tjs,
         @Nullable ParentChildCondition parentChildCond,
         Function<String,String> propNameDefaultFn,
         String queryName,
         String queryPart
      )
   {
      BaseQuery baseQuery =
         makeBaseQuery(tjs, parentChildCond, false, propNameDefaultFn, queryName, queryPart);

      String rowObjExpr =
         sqlDialect.getRowObjectExpression(
            baseQuery.getResultColumnMetadatas(),
            "q"
         );

      return
         "select\n" +
            indent(rowObjExpr) + " json\n" +
         "from (\n" +
            indent(baseQuery.getSql()) + "\n" +
         ") q";
   }

   private String makeChildRecordsQuery
      (
         ChildCollectionSpec childSpec,
         RelId parentRelId,
         String parentAlias,
         Function<String,String> propNameDefaultFn,
         String queryName,
         String queryPart
      )
   {
      TableJsonSpec tjs = childSpec.getTableJson();

      RelId childRelId = identifyTable(tjs.getTable(), queryName, queryPart);

      ParentChildCondition pcCond =
         getChildCollectionJoinCondition(childSpec, childRelId, parentRelId, parentAlias, queryName, queryPart);

      boolean unwrapChildValues = childSpec.getUnwrap();
      if ( unwrapChildValues && childSpec.getTableJson().getJsonPropertiesCount() > 1 )
         throw specError(
            queryName,
            queryPart,
            "Child collection rows cannot have unwrapped option specified where " +
             "multiple field expressions are included."
         );

      return
         makeAggregatedJsonResultSql(tjs, pcCond, propNameDefaultFn, unwrapChildValues, queryName, queryPart);
   }

   private ParentChildCondition getChildCollectionJoinCondition
      (
         ChildCollectionSpec childCollectionSpec,
         RelId childRelId,
         RelId parentRelId,
         String parentAlias,
         String queryName,
         String queryPart
      )
   {
      @Nullable CustomJoinCondition customJoinCond = childCollectionSpec.getCustomJoinCondition();
      if ( customJoinCond != null ) // custom join condition specified
      {
         if ( childCollectionSpec.getForeignKeyFields() != null )
            throw specError(
               queryName,
               queryPart,
               "Child collection cannot specify both customJoinCondition and foreignKeyFields."
            );

         String customJoinPart = joinPartDescriptions(queryPart, "custom join condition");
         validateCustomJoinCondition(parentRelId, childRelId, customJoinCond, queryName, customJoinPart);

         return customJoinCond.asChildFkCondition(parentAlias, dbmd);
      }
      else // foreign key join condition
      {
         @Nullable Set<String> fkFields = childCollectionSpec.getForeignKeyFieldsSet();
         ForeignKey fk = getForeignKey(childRelId, parentRelId, fkFields, queryName, queryPart);

         return new ChildFkCondition(parentAlias, fk.getForeignKeyComponents());
      }
   }

   private SqlQueryParts getInlineParentBaseQueryParts
      (
         InlineParentSpec inlineParentTableSpec,
         RelId childRelId,
         String childAlias,
         Set<String> avoidAliases,
         Function<String,String> propNameDefaultFn,
         String queryName,
         String queryPart
      )
   {
      SqlQueryParts q = new SqlQueryParts();

      TableJsonSpec tjs = inlineParentTableSpec.getTableJson();

      RelId parentRelId = identifyTable(tjs.getTable(), queryName, queryPart);

      BaseQuery fromClauseQuery =
         makeBaseQuery(tjs, null, true, propNameDefaultFn, queryName, queryPart);

      String fromClauseQueryAlias = StringFuns.makeNameNotInSet("q", avoidAliases);
      q.addAliasToScope(fromClauseQueryAlias);

      for ( ColumnMetadata parentColumn : fromClauseQuery.getResultColumnMetadatas() )
         q.addSelectClauseEntry(
            fromClauseQueryAlias + "." + parentColumn.getName(),
             parentColumn.getName(),
            SelectClauseEntry.Source.INLINE_PARENT
         );

      ParentPkCondition parentPkCond;
      @Nullable CustomJoinCondition customJoinCond = inlineParentTableSpec.getCustomJoinCondition();
      if ( customJoinCond != null )
      {
         if ( inlineParentTableSpec.getChildForeignKeyFieldsSet() != null )
            throw specError(
               queryName,
               queryPart,
               "The join to the parent table cannot include both a custom join condition " +
                "and foreign key fields."
            );

         parentPkCond = customJoinCond.asParentPkCondition(childAlias, dbmd);
      }
      else
      {
         @Nullable Set<String> fkFields = inlineParentTableSpec.getChildForeignKeyFieldsSet();
         ForeignKey fk = getForeignKey(childRelId, parentRelId, fkFields, queryName, queryPart);
         parentPkCond = new ParentPkCondition(childAlias, fk.getForeignKeyComponents());
      }

      String joinCond = parentPkCond.asEquationConditionOn(fromClauseQueryAlias, dbmd, HIDDEN_PK_PREFIX);

      q.addFromClauseEntry(
         "left join (\n" +
            indent(fromClauseQuery.getSql()) + "\n" +
         ") " + fromClauseQueryAlias + " on " + joinCond
      );

      return q;
   }

   private SqlQueryParts getReferencedParentBaseQueryParts
      (
         ReferencedParentSpec parentSpec,
         RelId childRelId,
         String childAlias,
         Function<String,String> propNameDefaultFn,
         String queryName,
         String queryPart
      )
   {
      // a referenced parent only requires a SELECT clause entry
      String parentRecordQuery =
         makeParentRecordQuery(parentSpec, childRelId, childAlias, propNameDefaultFn, queryName, queryPart);

      return new
         SqlQueryParts(
            singletonList(new SelectClauseEntry(
               "(\n" + indent(parentRecordQuery) + "\n)",
               dbmd.quoteIfNeeded(parentSpec.getReferenceName()),
               SelectClauseEntry.Source.PARENT_REFERENCE
            )),
            emptyList(), emptyList(), emptySet()
         );
   }

   private String makeParentRecordQuery
      (
         ReferencedParentSpec refdParentSpec,
         RelId childRelId,
         String childAlias,
         Function<String,String> propNameDefaultFn,
         String queryName,
         String queryPart
      )
   {
      TableJsonSpec tjs = refdParentSpec.getParentTableJsonSpec();

      RelId relId = identifyTable(tjs.getTable(), queryName, queryPart);

      ParentChildCondition childCondOnParent;
      @Nullable CustomJoinCondition customJoinCond = refdParentSpec.getCustomJoinCondition();
      if ( customJoinCond != null )
      {
         if ( refdParentSpec.getChildForeignKeyFieldsSet() != null )
            throw specError(
               queryName,
               queryPart,
               "The join to the parent table cannot include both a custom join condition " +
                  "and foreign key fields."
            );

         childCondOnParent = customJoinCond.asParentPkCondition(childAlias, dbmd);
      }
      else
      {
         @Nullable Set<String> fkFields = refdParentSpec.getChildForeignKeyFieldsSet();
         ForeignKey fk = getForeignKey(childRelId, relId, fkFields, queryName, queryPart);

         childCondOnParent = new ParentPkCondition(childAlias, fk.getForeignKeyComponents());
      }

      return
         makeJsonObjectRowsSql(tjs, childCondOnParent, propNameDefaultFn, queryName, queryPart);
   }

   private String makeSqlFromParts(SqlQueryParts q)
   {
      String selectEntriesStr =
         q.getSelectClauseEntries().stream()
         .map(p -> p.getValueExpression() + (p.getName().startsWith("\"") ? " " : " as ") + p.getName())
         .collect(joining(",\n"));

      String fromEntriesStr = String.join("\n", q.getFromClauseEntries());

      String whereEntriesStr = String.join(" and\n", q.getWhereClauseEntries());

      return
         "select\n" +
            indent(selectEntriesStr) + "\n" +
         "from\n" +
            indent(fromEntriesStr) + "\n" +
         (q.getWhereClauseEntries().isEmpty() ? "":
         "where (\n" +
            indent(whereEntriesStr) + "\n" +
         ")");
   }

   private @Nullable String getWhereCondition
      (
         TableJsonSpec tableJsonSpec,
         String tableAlias,
         String queryName,
         String queryPart
      )
   {
      List<String> conds = new ArrayList<>();

      List<String> paramFieldNames =
         tableJsonSpec.getFieldParamConditions().stream().map(FieldParamCondition::getField).collect(toList());

      verifyTableFieldsExist(
         tableJsonSpec.getTable(),
         defaultSchema,
         paramFieldNames,
         dbmd,
         queriesSource,
         queryName,
         queryPart
      );

      for ( FieldParamCondition fieldParamCond : tableJsonSpec.getFieldParamConditions() )
      {
         conds.add(
            sqlDialect.getFieldParamConditionSql(
                fieldParamCond,
                tableAlias,
                NAMED,
                getDefaultParamNameFn(tableJsonSpec.getTable(), fieldParamCond.getOp())
            )
         );
      }

      ifPresent(tableJsonSpec.getRecordCondition(), cond -> {
         String tableAliasVar = valueOr(cond.getWithTableAliasAs(), DEFAULT_TABLE_ALIAS_VAR);
         conds.add("(" + cond.getSql().replace(tableAliasVar, tableAlias) + ")");
      });

      return conds.isEmpty() ? null : String.join("\nand\n", conds);
   }

   private String getTableFieldExpressionForAlias
      (
         TableFieldExpr tableFieldExpr,
         String tableAlias
      )
   {
      if ( tableFieldExpr.isSimpleField() )
         return tableAlias + "." + tableFieldExpr.getField();
      else
      {
         String tableAliasVarInExpr = valueOr(tableFieldExpr.getWithTableAliasAs(), DEFAULT_TABLE_ALIAS_VAR);
         return tableFieldExpr.getExpression().replace(tableAliasVarInExpr, tableAlias);
      }
   }

   /// The returned function creates a default param name from a field name in the
   /// context of a given table..
   private Function<String,String> getDefaultParamNameFn(String tableName, FieldParamCondition.Operator op)
   {
      return (String fieldName) -> {
         String baseName = lowerCamelCase(tableName) + upperCamelCase(fieldName);
         if ( op.acceptsListParam() )
            return baseName + "List";
         else
            return baseName;
      };
   }

   private List<String> getAllParamNames(QuerySpec querySpec)
   {
      return getAllParamNames(querySpec.getTableJson());
   }

   private List<String> getAllParamNames(TableJsonSpec tableJsonSpec)
   {
      List<String> paramNames = new ArrayList<>();

      tableJsonSpec.getFieldParamConditions().stream()
         .map(fpCond ->
            valueOr(fpCond.getParamName(),
                    getDefaultParamNameFn(tableJsonSpec.getTable(), fpCond.getOp()).apply(fpCond.getField())))
         .forEach(paramNames::add);

      for ( ChildCollectionSpec childSpec: tableJsonSpec.getChildTableCollections() )
         paramNames.addAll(getAllParamNames(childSpec.getTableJson()));

      for ( InlineParentSpec parentSpec : tableJsonSpec.getInlineParentTables() )
         paramNames.addAll(getAllParamNames(parentSpec.getParentTableJsonSpec()));

      for ( ReferencedParentSpec parentSpec : tableJsonSpec.getReferencedParentTables() )
         paramNames.addAll(getAllParamNames(parentSpec.getParentTableJsonSpec()));

      @Nullable RecordCondition recCond = tableJsonSpec.getRecordCondition();
      if ( recCond != null )
         paramNames.addAll(recCond.getParamNames());

      return paramNames;
   }

   private ForeignKey getForeignKey
      (
         RelId childRelId,
         RelId parentRelId,
         @Nullable Set<String> foreignKeyFields,
         String queryName,
         String queryPart
      )
   {
      @Nullable ForeignKey fk =
         dbmd.getForeignKeyFromTo(childRelId, parentRelId, foreignKeyFields, REGISTERED_TABLES_ONLY);

      if ( fk == null )
      {
         String viaFkFields = foreignKeyFields != null ? "foreign keys " + foreignKeyFields
            : "implicit foreign key fields";

         throw specError(
            queryName,
            queryPart,
            "Foreign key not found from " + childRelId.getName() +
             " to " + parentRelId.getName() + " via " + viaFkFields + "."
         );
      }

      return fk;
   }

   private void validateCustomJoinCondition
      (
         RelId parentRelId,
         RelId childRelId,
         CustomJoinCondition customJoinCond,
         String queryName,
         String queryPart
      )
      throws StatementSpecificationException
   {
      @Nullable RelMetadata parentMd = dbmd.getRelationMetadata(parentRelId);
      @Nullable RelMetadata childMd = dbmd.getRelationMetadata(childRelId);

      if ( parentMd == null )
         throw specError(queryName, queryPart + " / custom join condition", "Parent not found." );
      if ( childMd == null )
         throw specError(queryName, queryPart + " / custom join condition", "Child not found." );

      List<String> parentMatchFields =
         customJoinCond.getEquatedFields().stream()
         .map(CustomJoinCondition.FieldPair::getParentPrimaryKeyField)
         .collect(toList());

      verifyTableFieldsExist(parentMatchFields, parentMd, dbmd, queriesSource, queryName, queryPart);

      List<String> childMatchFields =
         customJoinCond.getEquatedFields().stream()
         .map(CustomJoinCondition.FieldPair::getChildField)
         .collect(toList());

      verifyTableFieldsExist(childMatchFields, childMd, dbmd, queriesSource, queryName, queryPart);
   }

   private String getJsonPropertyName
      (
         TableFieldExpr tfe,
         Function<String,String> defaultFn,
         String tableName
      )
   {
      if ( tfe.isSimpleField() )
         return valueOrGet(tfe.getJsonProperty(), () -> defaultFn.apply(tfe.getField()));
      else
         return valueOrThrow(tfe.getJsonProperty(), () -> // expression fields must have output name specified
            new RuntimeException(
               "Json propery name is required for expression field " + tfe.getExpression() +
               " of table " + tableName + "."
            )
         );
   }

   /// Return a possibly qualified identifier for the given relation, omitting the schema
   /// qualifier if it has a schema for which it's specified to use unqualified names.
   private String minimalRelIdentifier(RelId relId)
   {
      @Nullable String schema = relId.getSchema();
      if ( schema == null || generateUnqualifiedNamesForSchemas.contains(dbmd.normalizeName(schema)) )
         return relId.getName();
      else
         return relId.getIdString();
   }

   private String indent(String s)
   {
      return StringFuns.indentLines(s, indentSpaces, true);
   }

   private static class BaseQuery
   {
      private final String sql;
      private final List<ColumnMetadata> resultColumnMetadatas;

      BaseQuery(String sql, List<ColumnMetadata> resultColumnMetadatas)
      {
         this.sql = sql;
         this.resultColumnMetadatas = List.copyOf(resultColumnMetadatas);
      }

      String getSql() { return sql; }

      List<ColumnMetadata> getResultColumnMetadatas() { return resultColumnMetadatas; }
   }

   private StatementSpecificationException specError
      (
         String queryName,
         String queryPart,
         String problem
      )
   {
      return new
         StatementSpecificationException(
         queriesSource,
            queryName,
            queryPart,
            problem
         );
   }

   private RelId identifyTable
      (
         String table,
         String statementName,
         String locationInStatement
      )
   {
      return
         identifySpecificationTable(
            table,
            defaultSchema,
            dbmd,
            queriesSource,
            statementName,
            locationInStatement
         );
   }

   private void verifySimpleSelectFieldsExist
      (
         TableJsonSpec tableJsonSpec,
         String queryName,
         String queryPart
      )
   {
      List<String> simpleSelectFields =
         tableJsonSpec.getFieldExpressions().stream()
         .filter(TableFieldExpr::isSimpleField)
         .map(TableFieldExpr::getField)
         .collect(toList());

      verifyTableFieldsExist(
         tableJsonSpec.getTable(),
         defaultSchema,
         simpleSelectFields,
         dbmd,
         queriesSource,
         queryName,
         queryPart
      );
   }

   String joinPartDescriptions(String part1, String part2)
   {
      return part1.isEmpty() ? part2 : part1 + " / " + part2;
   }
}

