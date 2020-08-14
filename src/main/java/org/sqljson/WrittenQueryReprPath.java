package org.sqljson;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toMap;
import org.checkerframework.checker.nullness.qual.Nullable;

import org.sqljson.specs.queries.ResultsRepr;


public class WrittenQueryReprPath
{
   private String queryName;
   private ResultsRepr resultsRepr;
   private @Nullable Path outputFilePath;

   public WrittenQueryReprPath
      (
         String queryName,
         ResultsRepr resultsRepr,
         @Nullable Path outputFilePath
      )
   {
      this.queryName = queryName;
      this.resultsRepr = resultsRepr;
      this.outputFilePath = outputFilePath;
   }

   public String getQueryName() { return queryName; }

   public ResultsRepr getResultsRepr() { return resultsRepr; }

   public @Nullable Path getOutputFilePath() { return outputFilePath; }


   public static Map<ResultsRepr, Path> getWrittenSqlPathsForQuery
      (
         String queryName,
         List<WrittenQueryReprPath> writtenPathsAllQueries
      )
   {
      return
         writtenPathsAllQueries.stream()
         .filter(wqr -> wqr.getQueryName().equals(queryName) && wqr.getOutputFilePath() != null)
         .collect(toMap(WrittenQueryReprPath::getResultsRepr, wqr -> requireNonNull(wqr.getOutputFilePath())));
   }
}

