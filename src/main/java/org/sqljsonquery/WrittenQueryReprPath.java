package org.sqljsonquery;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import static java.util.stream.Collectors.toMap;

import org.sqljsonquery.query_spec.ResultsRepr;


public class WrittenQueryReprPath
{
   private String queryName;
   private ResultsRepr resultsRepr;
   private Optional<Path> outputFilePath;

   public WrittenQueryReprPath
   (
      String queryName,
      ResultsRepr resultsRepr,
      Optional<Path> outputFilePath
   )
   {
      this.queryName = queryName;
      this.resultsRepr = resultsRepr;
      this.outputFilePath = outputFilePath;
   }

   public String getQueryName() { return queryName; }

   public ResultsRepr getResultsRepr() { return resultsRepr; }

   public Optional<Path> getOutputFilePath() { return outputFilePath; }


   public static Map<ResultsRepr,Path> writtenPathsForQuery
   (
      String queryName,
      List<WrittenQueryReprPath> writtenPathsAllQueries
   )
   {
      return
         writtenPathsAllQueries.stream()
         .filter(wqr -> wqr.getQueryName().equals(queryName) && wqr.getOutputFilePath().isPresent())
         .collect(toMap(WrittenQueryReprPath::getResultsRepr, wqr -> wqr.getOutputFilePath().get()));
   }

}

