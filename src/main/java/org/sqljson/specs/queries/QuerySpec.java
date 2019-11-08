package org.sqljson.specs.queries;

import java.util.ArrayList;
import java.util.List;
import static java.util.Collections.*;

import static org.sqljson.specs.queries.ResultsRepr.JSON_OBJECT_ROWS;


public final class QuerySpec
{
   private String queryName;
   private List<ResultsRepr> resultsRepresentations = singletonList(JSON_OBJECT_ROWS);
   private boolean generateResultTypes;
   private TableOutputSpec tableOutputSpec;

   private QuerySpec() {}

   public QuerySpec
   (
      String queryName,
      List<ResultsRepr> resultsRepresentations,
      boolean generateResultTypes,
      TableOutputSpec tableOutputSpec
   )
   {
      this.queryName = queryName;
      this.resultsRepresentations = unmodifiableList(new ArrayList<>(resultsRepresentations));
      this.generateResultTypes = generateResultTypes;
      this.tableOutputSpec = tableOutputSpec;
   }

   public String getQueryName() { return queryName; }

   /// Generates a SQL query for each of the specified result representations.
   public List<ResultsRepr> getResultsRepresentations() { return resultsRepresentations; }

   public boolean getGenerateResultTypes() { return generateResultTypes; }

   public TableOutputSpec getTableOutputSpec() { return tableOutputSpec; }
}
