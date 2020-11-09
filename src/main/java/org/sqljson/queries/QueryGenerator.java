package org.sqljson.queries;

import java.util.*;
import java.util.function.Function;
import static java.util.Collections.*;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.*;
import static java.util.function.Function.identity;

import org.checkerframework.checker.nullness.qual.Nullable;

import org.sqljson.common.StatementLocation;
import org.sqljson.common.StatementSpecificationException;
import org.sqljson.dbmd.*;
import org.sqljson.queries.result_types.GeneratedType;
import org.sqljson.queries.specs.*;
import org.sqljson.queries.sql.*;
import org.sqljson.common.util.StringFuns;
import org.sqljson.queries.sql.dialects.SqlDialect;
import static org.sqljson.common.util.Nullables.*;
import static org.sqljson.common.util.StatementValidations.*;
import static org.sqljson.common.util.StringFuns.indentLines;
import static org.sqljson.dbmd.ForeignKeyScope.REGISTERED_TABLES_ONLY;


public class QueryGenerator
{
   private final DatabaseMetadata dbmd;
   private final SqlDialect sqlDialect;
   private final @Nullable String defaultSchema;
   private final Set<String> unqualifiedNamesSchemas; // Use unqualified names for objects in these schemas.
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
         Set<String> unqualifiedNamesSchemas,
         Function<String,String> defaultPropNameFn,
         String queriesSource
      )
   {
      this.dbmd = dbmd;
      this.indentSpaces = 2;
      this.sqlDialect = SqlDialect.fromDatabaseMetadata(this.dbmd, this.indentSpaces);
      this.defaultSchema = defaultSchema;
      this.unqualifiedNamesSchemas = unqualifiedNamesSchemas.stream().map(dbmd::normalizeName).collect(toSet());
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
      Function<String,String> propNameFn =
         applyOr(querySpec.getOutputFieldNameDefault(), OutputFieldNameDefault::toFunctionOfFieldName,
                 this.defaultPropNameFn);

      Map<ResultsRepr,String> querySqls =
         querySpec.getResultsRepresentationsList().stream()
         .collect(toMap(identity(), repr -> makeQuerySql(querySpec, repr, propNameFn)));

      List<GeneratedType> generatedTypes = querySpec.getGenerateResultTypesOrDeault() ?
            queryTypesGenerator.generateTypes(querySpec.getTableJson(), emptyMap())
            : emptyList();

      return new
         GeneratedQuery(
            queryName,
            querySqls,
            generatedTypes,
            querySpec.getGenerateSourceOrDefault(),
            querySpec.getTypesFileHeader(),
            getAllParamNames(querySpec)
         );
   }

   private String makeQuerySql
      (
         QuerySpec querySpec,
         ResultsRepr resultsRepr,
         Function<String,String> propNameFn
      )
   {
      TableJsonSpec tableSpec = querySpec.getTableJson();
      String queryName = querySpec.getQueryName();

      switch ( resultsRepr )
      {
         case MULTI_COLUMN_ROWS:
         {
            return
               makeBaseQuery(
                  tableSpec,
                  null,
                  false,
                  propNameFn,
                  querySpec.getOrderBy(),
                  new StatementLocation(queryName)).sql +
               (querySpec.getForUpdateOrDefault() ? "\nfor update" : "");
         }
         case JSON_OBJECT_ROWS:
         {
            if ( querySpec.getForUpdateOrDefault() )
               throw specError(querySpec, "for update clause", "FOR UPDATE is only allowed for MULTI_COLUMN_ROWS results");

            return
               makeJsonObjectRowsSql(
                  tableSpec,
                  null,
                  propNameFn,
                  querySpec.getOrderBy(),
                  new StatementLocation(queryName)
               );
         }
         case JSON_ARRAY_ROW:
         {
            if ( querySpec.getForUpdateOrDefault() )
               throw specError(querySpec, "for update clause", "FOR UPDATE is only allowed for MULTI_COLUMN_ROWS results");

            return
               makeAggregatedJsonResultSql(
                  tableSpec,
                  null,
                  propNameFn,
                  false,
                  querySpec.getOrderBy(),
                  new StatementLocation(queryName)
               );
         }
         default:
            throw specError(querySpec, "resultsRepresentations", "Results representation is not valid.");
      }
   }

   /** Generate SQL and column name metadata for the given table output
    *  specification and parent/child condition, with multi-column and multi-row
    *  representation of results.
    * @param tableSpec
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
         TableJsonSpec tableSpec,
         @Nullable ParentChildCondition parentChildCond,
         boolean exportPkFieldsHidden,
         Function<String,String> propNameFn,
         @Nullable String orderBy,
         StatementLocation stmtLoc
      )
   {
      SqlQueryParts q = new SqlQueryParts();

      RelId relId = identifyTable(tableSpec.getTable(), stmtLoc);

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
      verifySimpleSelectFieldsExist(tableSpec, defaultSchema, dbmd, queriesSource, stmtLoc);
      for ( TableFieldExpr tfe : tableSpec.getFieldExpressionsList() )
         q.addSelectClauseEntry(
            getTableFieldExpressionSql(tfe, alias),
            dbmd.quoteIfNeeded(getJsonPropertyName(tfe, propNameFn, stmtLoc.withPart("table " + tableSpec.getTable()))),
            SelectClauseEntry.Source.NATIVE_FIELD
         );

      // Add child record collections to the select clause.
      for ( ChildCollectionSpec childSpec : tableSpec.getChildTableCollectionsList() )
      {
         StatementLocation childLoc =  stmtLoc.withPart("child collection '" + childSpec.getCollectionName() + "'");
         String childQuery = makeChildRecordsQuery(childSpec, relId, alias, propNameFn, childLoc);
         q.addSelectClauseEntry(
            lineCommentChildCollectionSelectExpression(childSpec) + "\n" +
            "(" + "\n" +
               indent(childQuery) + "\n" +
            ")",
            dbmd.quoteIfNeeded(childSpec.getCollectionName()),
            SelectClauseEntry.Source.CHILD_COLLECTION
         );
      }

      // Add query parts for inline parents.
      for ( InlineParentSpec parentSpec : tableSpec.getInlineParentTablesList() )
      {
         StatementLocation parentLoc = stmtLoc.withPart("inline parent '" + parentSpec.getTableJson().getTable() + "'");
         Set<String> aliases = q.getAliasesInScope();
         q.addParts(
            getInlineParentBaseQueryParts(parentSpec, relId, alias, aliases, propNameFn, parentLoc)
         );
      }

      // Add parts for referenced parents.
      for ( ReferencedParentSpec parentSpec : tableSpec.getReferencedParentTablesList() )
      {
         StatementLocation parentLoc = stmtLoc.withPart("parent '" + parentSpec.getTableJson().getTable() + "'");
         q.addParts(
            getReferencedParentBaseQueryParts(parentSpec, relId, alias, propNameFn, parentLoc)
         );
      }

      // Add parent/child relationship filter condition if any to the where clause.
      ifPresent(parentChildCond, pcCond ->
         q.addWhereClauseEntry(pcCond.asEquationConditionOn(alias, dbmd))
      );

      @Nullable String whereCond = getWhereConditionSql(tableSpec, alias);
      ifPresent(whereCond, q::addWhereClauseEntry);

      if ( orderBy != null )
         q.setOrderBy(orderBy);

      List<ColumnMetadata> columnMetadatas =
         q.getSelectClauseEntries().stream()
         .filter(e -> e.getSource() != SelectClauseEntry.Source.HIDDEN_PK)
         .map(e -> new ColumnMetadata(e.getName()))
         .collect(toList());

      return new BaseQuery(q.toSql(indentSpaces), columnMetadatas);
   }

   /** Make a query having a single row and column result, with the result value
    *  representing the collection of json object representations of all rows
    *  of the table whose output specification is passed.
    * @param tableSpec  The output specification for this table, the subject of the query.
    * @param parentChildCond A filter condition on this table (always) from a parent or child table whose alias
    *                        (accessible from the condition) can be assumed to be in context.
    * @return the generated SQL query
    */
   private String makeAggregatedJsonResultSql
      (
         TableJsonSpec tableSpec,
         @Nullable ParentChildCondition parentChildCond,
         Function<String,String> propNameFn,
         boolean unwrapSingleColumnValues,
         @Nullable String orderBy,
         StatementLocation stmtLoc
      )
   {
      BaseQuery baseQuery = makeBaseQuery(tableSpec, parentChildCond, false, propNameFn, null, stmtLoc);

      if ( unwrapSingleColumnValues && baseQuery.resultColumnMetadatas.size() != 1 )
         throw specError(stmtLoc,
            "Collection of " + tableSpec.getTable() + " rows cannot have " +
             "unwrapped option specified where multiple field expressions are included."
         );

      String aggExpr = unwrapSingleColumnValues ?
          sqlDialect.getAggregatedColumnValuesExpression(baseQuery.resultColumnMetadatas.get(0), orderBy, "q")
          : sqlDialect.getAggregatedRowObjectsExpression(baseQuery.resultColumnMetadatas, orderBy, "q");

      return
         "select\n" +
            indent(lineCommentAggregatedRowObjects(tableSpec)) + "\n" +
            indent(aggExpr) + " json\n" +
         "from (\n" +
            indent(lineCommentBaseTableQuery(tableSpec)) + "\n" +
            indent(baseQuery.sql) + "\n" +
         ") q";
   }

   /** Make a query having JSON object result values at the top level of the
    *  result set. The query returns a JSON value in a single column and with
    *  any number of result rows.
    * @param tableSpec  The output specification for this table, the subject of the query.
    * @param parentChildCond A filter condition on this table (always) from a parent or child table whose alias
    *                        (accessible from the condition) can be assumed to be in context.
    * @return the generated SQL query
    */
   private String makeJsonObjectRowsSql
      (
         TableJsonSpec tableSpec,
         @Nullable ParentChildCondition parentChildCond,
         Function<String,String> propNameFn,
         @Nullable String orderBy,
         StatementLocation stmtLoc
      )
   {
      BaseQuery baseQuery =
         makeBaseQuery(
            tableSpec,
            parentChildCond,
            false,
            propNameFn,
            null,
            stmtLoc
         );

      String rowObjExpr = sqlDialect.getRowObjectExpression(baseQuery.resultColumnMetadatas, "q");

      return
         "select\n" +
            indent(lineCommentTableRowObject(tableSpec)) + "\n" +
            indent(rowObjExpr) + " json\n" +
         "from (\n" +
            indent(lineCommentBaseTableQuery(tableSpec)) + "\n" +
            indent(baseQuery.sql) + "\n" +
         ") q" +
         (orderBy != null ? "\norder by " + orderBy.replace("$$", "q") : "");
   }

   private String makeChildRecordsQuery
      (
         ChildCollectionSpec childSpec,
         RelId parentRelId,
         String parentAlias,
         Function<String,String> propNameFn,
         StatementLocation stmtLoc
      )
   {
      TableJsonSpec tableSpec = childSpec.getTableJson();

      RelId childRelId = identifyTable(tableSpec.getTable(), stmtLoc);

      ChildFkCondition pcCond = getChildFkCondition(childSpec, childRelId, parentRelId, parentAlias, stmtLoc);

      boolean unwrapChildValues = valueOr(childSpec.getUnwrap(), false);
      if ( unwrapChildValues && childSpec.getTableJson().getJsonPropertiesCount() > 1 )
         throw specError(stmtLoc, "Unwrapped child collection option is incompatible with multiple field expressions.");

      return makeAggregatedJsonResultSql(tableSpec, pcCond, propNameFn, unwrapChildValues, childSpec.getOrderBy(), stmtLoc);
   }

   private SqlQueryParts getInlineParentBaseQueryParts
      (
         InlineParentSpec inlineParentSpec,
         RelId childRelId,
         String childAlias,
         Set<String> avoidAliases,
         Function<String,String> propNameFn,
         StatementLocation stmtLoc
      )
   {
      SqlQueryParts q = new SqlQueryParts();

      BaseQuery fromClauseQuery =
         makeBaseQuery(
            inlineParentSpec.getTableJson(),
            null,
            true,
            propNameFn,
            null,
            stmtLoc
         );

      String fromClauseQueryAlias = StringFuns.makeNameNotInSet("q", avoidAliases);
      q.addAliasToScope(fromClauseQueryAlias);

      for (int i = 0; i < fromClauseQuery.resultColumnMetadatas.size(); ++i )
      {
         ColumnMetadata parentColumn = fromClauseQuery.resultColumnMetadatas.get(i);
         q.addSelectClauseEntry(
            fromClauseQueryAlias + "." + parentColumn.getName(),
            parentColumn.getName(),
            SelectClauseEntry.Source.INLINE_PARENT,
            (i == 0 ? lineCommentInlineParentFieldsBegin(inlineParentSpec): null)
         );
      }

      String joinCond =
         getParentPkCondition(inlineParentSpec, childRelId, childAlias, stmtLoc)
         .asEquationConditionOn(fromClauseQueryAlias, dbmd, HIDDEN_PK_PREFIX);

      q.addFromClauseEntry(
         lineCommentJoinToParent(inlineParentSpec) + "\n" +
         "left join (\n" +
            indent(fromClauseQuery.sql) + "\n" +
         ") " + fromClauseQueryAlias + " on " + joinCond
      );

      return q;
   }

   private SqlQueryParts getReferencedParentBaseQueryParts
      (
         ReferencedParentSpec parentSpec,
         RelId childRelId,
         String childAlias,
         Function<String,String> propNameFn,
         StatementLocation stmtLoc
      )
   {
      String parentRecordQuery =
         makeJsonObjectRowsSql(
            parentSpec.getParentTableJsonSpec(),
            getParentPkCondition(parentSpec, childRelId, childAlias, stmtLoc),
            propNameFn,
            null,
            stmtLoc
         );

      // A referenced parent only requires a SELECT clause entry.
      return new SqlQueryParts(
         singletonList(new SelectClauseEntry(
            lineCommentReferencedParent(parentSpec) + "\n" +
            "(\n" +
               indent(parentRecordQuery) + "\n" +
            ")",
            dbmd.quoteIfNeeded(parentSpec.getReferenceName()),
            SelectClauseEntry.Source.PARENT_REFERENCE,
            null
         )),
         emptyList(), emptyList(), null, emptySet()
      );
   }

   private ParentPkCondition getParentPkCondition
      (
         ParentSpec parentSpec,
         RelId childRelId,
         String childAlias,
         StatementLocation stmtLoc
      )
   {
      @Nullable CustomJoinCondition customJoinCondition = parentSpec.getCustomJoinCondition();

      if ( customJoinCondition != null )
      {
         if ( parentSpec.getChildForeignKeyFieldsSet() != null )
            throw specError(stmtLoc, "Parent with customJoinCondition cannot specify foreignKeyFields.");

         return customJoinCondition.asParentPkCondition(childAlias, dbmd);
      }
      else
      {
         @Nullable Set<String> childForeignKeyFieldsSet = parentSpec.getChildForeignKeyFieldsSet();
         TableJsonSpec parentTableSpec = parentSpec.getParentTableJsonSpec();
         RelId parentRelId = identifyTable(parentTableSpec.getTable(), stmtLoc);
         ForeignKey fk = getForeignKey(childRelId, parentRelId, childForeignKeyFieldsSet, stmtLoc);
         return new ParentPkCondition(childAlias, fk.getForeignKeyComponents());
      }
   }

   private ChildFkCondition getChildFkCondition
      (
         ChildCollectionSpec childCollectionSpec,
         RelId childRelId,
         RelId parentRelId,
         String parentAlias,
         StatementLocation stmtLoc
      )
   {
      @Nullable CustomJoinCondition customJoinCond = childCollectionSpec.getCustomJoinCondition();

      if ( customJoinCond != null ) // custom join condition specified
      {
         if ( childCollectionSpec.getForeignKeyFields() != null )
            throw specError(stmtLoc, "Child collection that specifies customJoinCondition cannot specify foreignKeyFields.");

         validateCustomJoinCondition(customJoinCond, childRelId, parentRelId, dbmd, queriesSource, stmtLoc.withPart("custom join condition"));

         return customJoinCond.asChildFkCondition(parentAlias, dbmd);
      }
      else // foreign key join condition
      {
         @Nullable Set<String> fkFields = childCollectionSpec.getForeignKeyFieldsSet();
         ForeignKey fk = getForeignKey(childRelId, parentRelId, fkFields, stmtLoc);

         return new ChildFkCondition(parentAlias, fk.getForeignKeyComponents());
      }
   }

   private @Nullable String getWhereConditionSql
      (
         TableJsonSpec tableSpec,
         String tableAlias
      )
   {
      @Nullable RecordCondition cond = tableSpec.getRecordCondition();
      if ( cond != null )
      {
         String tableAliasVar = valueOr(cond.getWithTableAliasAs(), DEFAULT_TABLE_ALIAS_VAR);
         return "(" + cond.getSql().replace(tableAliasVar, tableAlias) + ")";
      }
      else
         return null;
   }

   private String getTableFieldExpressionSql
      (
         TableFieldExpr tableFieldExpr,
         String tableAlias
      )
   {
      if ( tableFieldExpr.getField() != null )
         return tableAlias + "." + requireNonNull(tableFieldExpr.getField());
      else
      {
         String tableAliasVarInExpr = valueOr(tableFieldExpr.getWithTableAliasAs(), DEFAULT_TABLE_ALIAS_VAR);
         assert tableFieldExpr.getExpression() != null;
         String expr = requireNonNull(tableFieldExpr.getExpression());
         return expr.replace(tableAliasVarInExpr, tableAlias);
      }
   }

   private List<String> getAllParamNames(QuerySpec querySpec)
   {
      return getAllParamNames(querySpec.getTableJson());
   }

   private List<String> getAllParamNames(TableJsonSpec tableSpec)
   {
      List<String> paramNames = new ArrayList<>();

      for ( ChildCollectionSpec childSpec: tableSpec.getChildTableCollectionsList() )
         paramNames.addAll(getAllParamNames(childSpec.getTableJson()));

      for ( InlineParentSpec parentSpec : tableSpec.getInlineParentTablesList() )
         paramNames.addAll(getAllParamNames(parentSpec.getParentTableJsonSpec()));

      for ( ReferencedParentSpec parentSpec : tableSpec.getReferencedParentTablesList() )
         paramNames.addAll(getAllParamNames(parentSpec.getParentTableJsonSpec()));

      @Nullable RecordCondition recCond = tableSpec.getRecordCondition();
      if ( recCond != null && recCond.getParamNames() != null )
         paramNames.addAll(requireNonNull(recCond.getParamNames()));

      return paramNames;
   }

   private ForeignKey getForeignKey
      (
         RelId childRelId,
         RelId parentRelId,
         @Nullable Set<String> foreignKeyFields,
         StatementLocation stmtLoc
      )
   {
      @Nullable ForeignKey fk = dbmd.getForeignKeyFromTo(childRelId, parentRelId, foreignKeyFields, REGISTERED_TABLES_ONLY);

      if ( fk == null )
      {
         throw specError(stmtLoc,
            "No foreign key found from " + childRelId.getName() + " to " + parentRelId.getName() + " via " +
            (foreignKeyFields != null ? "foreign keys " + foreignKeyFields : "implicit foreign key fields") + "."
         );
      }

      return fk;
   }

   private String getJsonPropertyName
      (
         TableFieldExpr tfe,
         Function<String,String> defaultFn,
         StatementLocation stmtLoc
      )
   {
      if ( tfe.getField() != null )
         return valueOrGet(tfe.getJsonProperty(), () -> defaultFn.apply(requireNonNull(tfe.getField())));
      else
      {
         assert tfe.getExpression() != null;
         return valueOrThrow(tfe.getJsonProperty(), () -> // expression fields must have output name specified
            specError(stmtLoc, "Json property required for expression field " + tfe.getExpression() + ".")
         );
      }
   }

   private String lineCommentTableRowObject(TableJsonSpec tableJsonSpec)
   {
      return "-- row object builder for table '" + tableJsonSpec.getTable() + "'";
   }

   private String lineCommentBaseTableQuery(TableJsonSpec tableSpec)
   {
      return "-- base query for table '" + tableSpec.getTable() + "'";
   }

   private String lineCommentAggregatedRowObjects(TableJsonSpec tableSpec)
   {
      return "-- aggregated row objects builder for table '" + tableSpec.getTable() + "'";
   }

   private String lineCommentChildCollectionSelectExpression(ChildCollectionSpec childSpec)
   {
      return "-- records from child table '" + childSpec.getTableJson().getTable() + "'" +
         " as collection '" + childSpec.getCollectionName() + "'";
   }

   private String lineCommentJoinToParent(ParentSpec parentSpec)
   {
      return
         "-- parent table '" + parentSpec.getTableJson().getTable() + "'" +
         ", joined for inlined fields";
   }

   private String lineCommentInlineParentFieldsBegin(ParentSpec parentSpec)
   {
      return "-- field(s) inlined from parent table '" + parentSpec.getTableJson().getTable() + "'";
   }

   private String lineCommentReferencedParent(ReferencedParentSpec parentSpec)
   {
      return
         "-- parent table '" + parentSpec.getTableJson().getTable() + "'" +
         " referenced as '" + parentSpec.getReferenceName() + "'";
   }

   /// Return a possibly qualified identifier for the given relation, omitting the schema
   /// qualifier if it has a schema for which it's specified to use unqualified names.
   private String minimalRelIdentifier(RelId relId)
   {
      @Nullable String schema = relId.getSchema();
      if ( schema == null || unqualifiedNamesSchemas.contains(dbmd.normalizeName(schema)) )
         return relId.getName();
      else
         return relId.getIdString();
   }

   private String indent(String s)
   {
      return indentLines(s, indentSpaces, true);
   }

   private StatementSpecificationException specError
      (
         StatementLocation stmtLoc,
         String problem
      )
   {
      return new StatementSpecificationException(queriesSource, stmtLoc, problem);
   }

   private StatementSpecificationException specError
      (
         QuerySpec querySpec,
         String queryPart,
         String problem
      )
   {
      return specError(new StatementLocation(querySpec.getQueryName(), queryPart), problem);
   }

   private RelId identifyTable
      (
         String table,
         StatementLocation loc
      )
   {
      return identifySpecificationTable(table, defaultSchema, dbmd, queriesSource, loc);
   }

   private static class BaseQuery
   {
      final String sql;
      final List<ColumnMetadata> resultColumnMetadatas;

      BaseQuery(String sql, List<ColumnMetadata> resultColumnMetadatas)
      {
         this.sql = sql;
         this.resultColumnMetadatas = List.copyOf(resultColumnMetadatas);
      }
   }
}

