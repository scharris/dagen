package org.sqljson.queries.specs;

import java.util.ArrayList;
import java.util.List;
import static java.util.Collections.*;

import org.checkerframework.checker.nullness.qual.Nullable;

import static org.sqljson.queries.specs.ResultsRepr.JSON_OBJECT_ROWS;


public final class QuerySpec
{
   private String queryName;
   private List<ResultsRepr> resultsRepresentations = singletonList(JSON_OBJECT_ROWS);
   private boolean generateResultTypes = true;
   private boolean generateSource = true; // Contains at least the resource name for generated SQL, if not result types.
   private @Nullable OutputFieldNameDefault outputFieldNameDefault = null; // inherited from query group spec if empty
   private TableJsonSpec tableJson;
   private boolean forUpdate = false;
   private @Nullable String typesFileHeader = null;

   private QuerySpec()
   {
      this.queryName = "";
      this.tableJson = new TableJsonSpec();
   }

   public QuerySpec
      (
         String queryName,
         List<ResultsRepr> resultsRepresentations,
         boolean generateResultTypes,
         boolean generateSource,
         @Nullable OutputFieldNameDefault outputFieldNameDefault,
         TableJsonSpec tableJson,
         boolean forUpdate,
         @Nullable String typesFileHeader
      )
   {
      this.queryName = queryName;
      this.resultsRepresentations = unmodifiableList(new ArrayList<>(resultsRepresentations));
      this.generateResultTypes = generateResultTypes;
      this.generateSource = generateSource;
      this.outputFieldNameDefault = outputFieldNameDefault;
      this.tableJson = tableJson;
      this.forUpdate = forUpdate;
      this.typesFileHeader = typesFileHeader;
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

   public @Nullable OutputFieldNameDefault getOutputFieldNameDefault() { return outputFieldNameDefault; }

   public TableJsonSpec getTableJson() { return tableJson; }

   public boolean getForUpdate() { return forUpdate; }

   public @Nullable String getTypesFileHeader() { return typesFileHeader; }
}