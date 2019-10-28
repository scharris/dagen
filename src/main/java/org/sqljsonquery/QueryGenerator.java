package org.sqljsonquery;

import java.util.*;
import java.util.function.Function;

import static java.util.Collections.*;
import static java.util.stream.Collectors.*;
import static java.util.Optional.empty;
import static java.util.function.Function.identity;

import org.sqljsonquery.queryspec.*;
import org.sqljsonquery.util.*;
import static org.sqljsonquery.util.StringFuns.*;
import static org.sqljsonquery.util.CollFuns.*;
import static org.sqljsonquery.util.Optionals.opt;
import org.sqljsonquery.dbmd.*;
import static org.sqljsonquery.dbmd.ForeignKeyScope.REGISTERED_TABLES_ONLY;
import org.sqljsonquery.sql.ChildFkCondition;
import org.sqljsonquery.sql.ParentChildCondition;
import org.sqljsonquery.sql.ParentPkCondition;
import org.sqljsonquery.sql.SqlQueryParts;
import org.sqljsonquery.sql.dialect.OracleDialect;
import org.sqljsonquery.sql.dialect.PostgresDialect;
import org.sqljsonquery.sql.dialect.SqlDialect;
import org.sqljsonquery.types.GeneratedType;


public class QueryGenerator
{
   private final DatabaseMetadata dbmd;
   private final Optional<String> defaultSchema;
   private final SqlDialect sqlDialect;
   private final int indentSpaces;
   private final TypesGenerator typesGenerator;
   private final Function<String,String> outputFieldNameDefaultFn;

   enum DbmsType { PG, ORA, ISO }

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
      Optional<String> defaultSchema,
      Function<String,String> outputFieldNameDefaultFn
   )
   {
      this.dbmd = dbmd;
      this.defaultSchema = defaultSchema;
      this.indentSpaces = 2;
      this.sqlDialect = getSqlDialect(this.dbmd, this.indentSpaces);
      this.typesGenerator = new TypesGenerator(dbmd, defaultSchema, outputFieldNameDefaultFn);
      this.outputFieldNameDefaultFn = outputFieldNameDefaultFn;
   }

   public List<SqlJsonQuery> generateSqlJsonQueries(List<QuerySpec> querySpecs)
   {
      return
         querySpecs.stream()
         .map(this::makeSqlJsonQuery)
         .collect(toList());
   }

   private SqlJsonQuery makeSqlJsonQuery(QuerySpec querySpec)
   {
      String queryName = querySpec.getQueryName();

      Map<ResultsRepr,String> querySqls =
         querySpec.getResultsRepresentations().stream()
         .collect(toMap(identity(), repr -> makeSqlForResultsRepr(querySpec, repr)));

      List<GeneratedType> generatedTypes =
         querySpec.getGenerateResultTypes() ?
            typesGenerator.generateTypes(querySpec.getTableOutputSpec(), emptySet())
            : emptyList();

      return new SqlJsonQuery(queryName, querySqls, generatedTypes);
   }

   private String makeSqlForResultsRepr(QuerySpec querySpec, ResultsRepr resultsRepr)
   {
      TableOutputSpec tos = querySpec.getTableOutputSpec();
      switch (resultsRepr)
      {
         case MULTI_COLUMN_ROWS: return makeBaseQuery(tos, empty(), false).getSql();
         case JSON_OBJECT_ROWS:  return makeJsonResultSql(tos, empty(), false);
         case JSON_ARRAY_ROW:    return makeJsonResultSql(tos, empty(), true);
         default: throw new RuntimeException("unrecognized results representation: " + resultsRepr);
      }
   }

   /** Generate SQL and column name metadata for the given table output
    *  specification and parent/child condition, with multi-column and multi-row
    *  representation of results.
    * @param tableOutputSpec
    *    The output specification for this table, the subject of the query.
    * @param parentChildCond
    *    A filter condition on this table from a parent or child table whose
    *    alias (accessible from the condition) can be assumed to be in context.
    * @param exportPkFields
    *    If enabled then columns will be added to the SQL select clause for any
    *    primary key fields that are not included in the table output
    *    as output fields. These columns will not be included in the result's
    *    resultColumnNames list. This is useful for filtering results for a
    *    parent/child relationship without having to include primary key fields
    *    in the columns included in result structures (which are selected at a
    *    level above this query).
    * @return
    *    A BaseQuery structure containing the generated SQL and some metadata
    *    about the query (e.g. column names).
    */
   private BaseQuery makeBaseQuery
   (
      TableOutputSpec tableOutputSpec,
      Optional<ParentChildCondition> parentChildCond,
      boolean exportPkFields
   )
   {
      SqlQueryParts q = new SqlQueryParts();

      // Identify this table and make an alias for it.
      RelId relId = dbmd.identifyTable(tableOutputSpec.getTableName(), defaultSchema);
      parentChildCond.ifPresent(pcCond ->
         q.addAliasToScope(pcCond.getOtherTableAlias())
      );
      String alias = q.makeNewAliasFor(relId.getName());
      q.addFromClauseEntry(relId.getIdString() + " " + alias);

      // If exporting pk fields, add any that aren't already in the output fields list to the select clause.
      Set<String> hiddenPkFields = new HashSet<>();
      if ( exportPkFields )
         for ( String pkFieldName : getOmittedPkFieldNames(relId, tableOutputSpec.getNativeFields()) )
         {
            String pkf = dbmd.quoteIfNeeded(pkFieldName);
            q.addSelectClauseEntry(alias + "." + pkf, pkf);
            hiddenPkFields.add(pkf);
         }

      // Add this table's own output fields to the select clause.
      for ( TableOutputField tof : tableOutputSpec.getNativeFields() )
         q.addSelectClauseEntry(
            alias + "." + tof.getDatabaseFieldName(), // db fields must be quoted as needed in queries spec itself
            dbmd.quoteIfNeeded(getOutputFieldName(tof, tof.getDatabaseFieldName()))
         );

      // Add child record collections to the select clause.
      for ( ChildCollectionSpec childCollectionSpec : tableOutputSpec.getChildCollections() )
         q.addSelectClauseEntry(
            "(\n" + indent(makeChildRecordsQuery(childCollectionSpec, relId, alias)) + "\n)",
            dbmd.quoteIfNeeded(childCollectionSpec.getChildCollectionName())
         );

      // Add query parts for inline parents.
      for ( InlineParentSpec inlineParentTableSpec : tableOutputSpec.getInlineParents() )
         q.addParts(getInlineParentBaseQueryParts(inlineParentTableSpec, relId, alias, q.getAliasesInScope()));

      // Add parts for referenced parents.
      for ( ReferencedParentSpec refdParentSpec : tableOutputSpec.getReferencedParents() )
         q.addParts(getReferencedParentBaseQueryParts(refdParentSpec, relId, alias));

      // Add parent/child relationship filter condition if any to the where clause.
      parentChildCond.ifPresent(pcCond ->
         q.addWhereClauseEntry(pcCond.asEquationConditionOn(alias, dbmd))
      );

      // Add general filter condition if provided to the WHERE clause.
      makeFilterConditionForAlias(tableOutputSpec.getFilter(), alias).ifPresent(filterCond ->
         q.addWhereClauseEntry("(" + filterCond + ")")
      );

      String sql = makeSqlFromParts(q);
      List<String> resultColumnNames = q.getSelectClauseEntries().stream().map(Pair::snd)
         .filter(f -> !hiddenPkFields.contains(f)).collect(toList());

      return new BaseQuery(sql, resultColumnNames);
   }

   /** Make a query having JSON result values at the top level of the result set.
       Depending on aggregateRows, the query either returns a single row with a
       single JSON array value column (when aggregateRows is true), or else one
       JSON value in each of any number of result rows. In all cases result sets
       have exactly one column.
    * @param tableOutputSpec  The output specification for this table, the subject of the query.
    * @param parentChildLinkCond A filter condition on this table (always) from a parent or child table whose alias
    *                            (accessible from the condition) can be assumed to be in context.
    * @return the generated SQL query
    */
   private String makeJsonResultSql
   (
      TableOutputSpec tableOutputSpec,
      Optional<ParentChildCondition> parentChildLinkCond,
      boolean aggregateRows
   )
   {
      BaseQuery baseQuery = makeBaseQuery(tableOutputSpec, parentChildLinkCond, false);

      String jsonValueSelectExpr =
         aggregateRows ?
            sqlDialect.getJsonAggregatedObjectsSelectExpression(baseQuery.getResultColumnNames(), "q") :
            sqlDialect.getJsonObjectSelectExpression(baseQuery.getResultColumnNames(), "q");

      return
         "select\n" +
            indent(jsonValueSelectExpr) + " json\n" +
         "from (\n" +
            indent(baseQuery.getSql()) + "\n" +
         ") q";
   }

   private String makeChildRecordsQuery
   (
      ChildCollectionSpec childCollectionSpec,
      RelId parentRelId,
      String parentAlias
   )
   {
      TableOutputSpec tos = childCollectionSpec.getChildTableOutputSpec();

      RelId relId = dbmd.identifyTable(tos.getTableName(), defaultSchema);

      ForeignKey fk = getForeignKey(relId, parentRelId, childCollectionSpec.getForeignKeyFieldsSet());

      ChildFkCondition childFkCond = new ChildFkCondition(parentAlias, fk.getForeignKeyComponents());

      return makeJsonResultSql(tos, opt(childFkCond), true);
   }

   private SqlQueryParts getInlineParentBaseQueryParts
   (
      InlineParentSpec inlineParentTableSpec,
      RelId childRelId,
      String childAlias,
      Set<String> avoidAliases
   )
   {
      SqlQueryParts q = new SqlQueryParts();

      TableOutputSpec tos = inlineParentTableSpec.getInlineParentTableOutputSpec();
      String table = tos.getTableName();
      RelId relId = dbmd.identifyTable(table, defaultSchema);
      ForeignKey fk = getForeignKey(childRelId, relId, inlineParentTableSpec.getChildForeignKeyFieldsSet());

      BaseQuery fromClauseQuery = makeBaseQuery(tos, empty(), true);

      String fromClauseQueryAlias = makeNameNotInSet("q", avoidAliases);
      q.addAliasToScope(fromClauseQueryAlias);

      for ( String parentCol : fromClauseQuery.getResultColumnNames() )
         q.addSelectClauseEntry(fromClauseQueryAlias + "." + parentCol, parentCol);

      ParentPkCondition parentPkCond = new ParentPkCondition(childAlias, fk.getForeignKeyComponents());
      q.addFromClauseEntry(
         "left join (\n" +
            indent(fromClauseQuery.getSql()) + "\n" +
            ") " + fromClauseQueryAlias + " on " + parentPkCond.asEquationConditionOn(fromClauseQueryAlias, dbmd)
      );

      return q;
   }

   private SqlQueryParts getReferencedParentBaseQueryParts
      (
         ReferencedParentSpec referencedParentSpec,
         RelId childRelId,
         String childAlias
      )
   {
      // a referenced parent only requires a SELECT clause entry
      return new SqlQueryParts(
         singletonList(Pair.make(
            "(\n" + indent(makeParentRecordQuery(referencedParentSpec, childRelId, childAlias)) + "\n)",
            dbmd.quoteIfNeeded(referencedParentSpec.getReferenceFieldName())
         )),
         emptyList(), emptyList(), emptySet()
      );
   }

   private String makeParentRecordQuery
   (
      ReferencedParentSpec refdParentSpec,
      RelId childRelId,
      String childAlias
   )
   {
      TableOutputSpec tos = refdParentSpec.getParentTableOutputSpec();

      RelId relId = dbmd.identifyTable(tos.getTableName(), defaultSchema);

      ForeignKey fk = getForeignKey(childRelId, relId, refdParentSpec.getChildForeignKeyFieldsSet());

      ParentPkCondition parentPkCond = new ParentPkCondition(childAlias, fk.getForeignKeyComponents());

      return makeJsonResultSql(tos, opt(parentPkCond), false);
   }

   public String makeSqlFromParts(SqlQueryParts q)
   {
      String selectEntriesStr =
         q.getSelectClauseEntries().stream()
         .map(p -> p.fst() + (p.snd().startsWith("\"") ? " " : " as ") + p.snd())
         .collect(joining(",\n"));

      String fromEntriesStr = String.join("\n", q.getFromClauseEntries());

      String whereEntriesStr = String.join(" and\n", q.getWhereClauseEntries());

      return
         "select\n" +
            indent(selectEntriesStr) + "\n" +
         "from\n" +
            indent(fromEntriesStr) + "\n" +
         (q.getWhereClauseEntries().isEmpty() ? "":
         "where\n" +
            indent(whereEntriesStr));
   }

   /// Return the set of primary key fields from database metadata which are not
   /// in the given output fields list (after normalization for the db).
   private Set<String> getOmittedPkFieldNames
   (
      RelId relId,
      List<TableOutputField> tableOutputFields
   )
   {
      // We assume the dbmd-reported pk fields are already case-normalized for the db, but the database field names
      // from the queries spec need to be normalized before comparison.
      Set<String> normdSpecDbFields =
         tableOutputFields.stream()
         .map(f -> dbmd.normalizeName(f.getDatabaseFieldName()))
         .collect(toSet());

      return setMinus(dbmd.getPrimaryKeyFieldNames(relId), normdSpecDbFields);
   }

   /**
    * Make a condition for inclusion in a SQL WHERE clause for the given filter if provided, and table/query alias.
    * The alias value is substituted in the filter expression in place of each occurrence of the alias variable.
    * @param filter The filter condition, in the form alias:expr. Only the first colon is considered as the separator,
    *               and any whitespace is trimmed from both the alias and expr sides prior to interpretation.
    * @param alias The alias of the relation which is to be substituted into the filter expression.
    * @return The condition suitable for inclusion in a SQL WHERE clause if filter was provided, else empty.
    */
   private Optional<String> makeFilterConditionForAlias(Optional<String> filter, String alias)
   {
      return filter.map(filterStr -> {
         int firstColonIx = filterStr.indexOf(':');
         if ( firstColonIx == -1 )
            throw new RuntimeException("improper filter format for filter '" + filter + "'.");
         String aliasVar = filterStr.substring(0, firstColonIx).trim();
         String filterExpr = filterStr.substring(firstColonIx+1).trim();
         return filterExpr.replace(aliasVar, alias);
      });
   }

   private ForeignKey getForeignKey
   (
      RelId childRelId,
      RelId parentRelId,
      Optional<Set<String>> foreignKeyFields
   )
   {
      return
         dbmd.getForeignKeyFromTo(childRelId, parentRelId, foreignKeyFields, REGISTERED_TABLES_ONLY)
         .orElseThrow(() -> new RuntimeException(
            "foreign key not found from " + childRelId.getName() + " to " + parentRelId.getName() +
            " via fks " + foreignKeyFields
         ));
   }

   private String getOutputFieldName(TableOutputField tof, String dbFieldName)
   {
      return tof.getOutputName().orElseGet(() -> outputFieldNameDefaultFn.apply(dbFieldName));
   }


   private String indent(String s)
   {
      return indentLines(s, indentSpaces, true);
   }

   private static DbmsType getDbmsType(String dbmsName)
   {
      String dbmsLower = dbmsName.toLowerCase();
      if ( dbmsLower.contains("postgres") ) return DbmsType.PG;
      else if ( dbmsLower.contains("oracle") ) return DbmsType.ORA;
      else return DbmsType.ISO;
   }

   private static SqlDialect getSqlDialect(DatabaseMetadata dbmd, int indentSpaces)
   {
      DbmsType dbmsType = getDbmsType(dbmd.getDbmsName());
      switch ( dbmsType )
      {
         case PG: return new PostgresDialect(indentSpaces);
         case ORA: return new OracleDialect(indentSpaces);
         default: throw new RuntimeException("dbms type " + dbmsType + " is currently not supported");
      }
   }

   private static class BaseQuery
   {
      private final String sql;
      private final List<String> resultColumnNames;

      public BaseQuery(String sql, List<String> resultColumnNames)
      {
         this.sql = sql;
         this.resultColumnNames = unmodifiableList(new ArrayList<>(resultColumnNames));
      }

      public String getSql() { return sql; }

      public List<String> getResultColumnNames() { return resultColumnNames; }
   }

}
