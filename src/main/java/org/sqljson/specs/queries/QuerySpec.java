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
   private boolean generateSource = true; // Contains at least the resource name for generated SQL, if not result types.
   private TableOutputSpec tableOutputSpec;

   private QuerySpec() {}

   public QuerySpec
   (
      String queryName,
      List<ResultsRepr> resultsRepresentations,
      boolean generateResultTypes,
      boolean generateSource,
      TableOutputSpec tableOutputSpec
   )
   {
      this.queryName = queryName;
      this.resultsRepresentations = unmodifiableList(new ArrayList<>(resultsRepresentations));
      this.generateResultTypes = generateResultTypes;
      this.generateSource = generateSource;
      this.tableOutputSpec = tableOutputSpec;
      if ( generateResultTypes && !generateSource )
         throw new RuntimeException(
            "In query \"" + queryName + "\", cannot generate result types without generateSource option enabled."
         );
   }

   public String getQueryName() { return queryName; }

   /// Generates a SQL query for each of the specified result representations.
   public List<ResultsRepr> getResultsRepresentations() { return resultsRepresentations; }

   public boolean getGenerateResultTypes() { return generateResultTypes; }

   public boolean getGenerateSource() { return generateSource; }

   public TableOutputSpec getTableOutputSpec() { return tableOutputSpec; }
}
