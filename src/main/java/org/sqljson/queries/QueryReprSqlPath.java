package org.sqljson.queries;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toMap;

import org.checkerframework.checker.nullness.qual.Nullable;

import org.sqljson.queries.specs.ResultsRepr;


public class QueryReprSqlPath
{
   private String queryName;
   private ResultsRepr resultsRepr;
   private @Nullable Path sqlPath;

   public QueryReprSqlPath
      (
         String queryName,
         ResultsRepr resultsRepr,
         @Nullable Path sqlPath
      )
   {
      this.queryName = queryName;
      this.resultsRepr = resultsRepr;
      this.sqlPath = sqlPath;
   }

   public String getQueryName() { return queryName; }

   public ResultsRepr getResultsRepr() { return resultsRepr; }

   public @Nullable Path getSqlPath() { return sqlPath; }

   public static Map<ResultsRepr,Path> getReprToSqlPathMapForQuery
      (
         String queryName,
         List<QueryReprSqlPath> writtenPathsAllQueries
      )
   {
      return
         writtenPathsAllQueries.stream()
         .filter(wqr -> wqr.getQueryName().equals(queryName) && wqr.getSqlPath() != null)
         .collect(toMap(QueryReprSqlPath::getResultsRepr, wqr -> requireNonNull(wqr.getSqlPath())));
   }
}

