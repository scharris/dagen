package org.sqljson.queries.specs;

import java.util.ArrayList;
import java.util.List;
import static java.util.Collections.*;

import org.checkerframework.checker.nullness.qual.Nullable;

import com.fasterxml.jackson.annotation.JsonIgnore;

import static org.sqljson.common.util.Nullables.valueOr;
import static org.sqljson.queries.specs.ResultsRepr.JSON_OBJECT_ROWS;


public final class QuerySpec
{
   private final String queryName;
   private final TableJsonSpec tableJson;
   private final @Nullable List<ResultsRepr> resultsRepresentations;
   private final @Nullable Boolean generateResultTypes;
   private final @Nullable Boolean generateSource; // Contains at least the resource name for generated SQL, if not result types.
   private final @Nullable OutputFieldNameDefault outputFieldNameDefault; // inherited from query group spec if empty
   private final @Nullable Boolean forUpdate;
   private final @Nullable String typesFileHeader;

   private QuerySpec()
   {
      this.queryName = "";
      this.tableJson = new TableJsonSpec();
      this.resultsRepresentations = singletonList(JSON_OBJECT_ROWS);
      this.generateResultTypes = true;
      this.generateSource = true;
      this.outputFieldNameDefault = null;
      this.forUpdate = false;
      this.typesFileHeader = null;
   }

   public QuerySpec
      (
         String queryName,
         TableJsonSpec tableJson,
         @Nullable List<ResultsRepr> resultsRepresentations,
         @Nullable Boolean generateResultTypes,
         @Nullable Boolean generateSource,
         @Nullable OutputFieldNameDefault outputFieldNameDefault,
         @Nullable Boolean forUpdate,
         @Nullable String typesFileHeader
      )
   {
      this.queryName = queryName;
      this.resultsRepresentations = resultsRepresentations != null ?
         unmodifiableList(new ArrayList<>(resultsRepresentations))
         : singletonList(JSON_OBJECT_ROWS);
      this.generateResultTypes = generateResultTypes;
      this.generateSource = generateSource;
      this.outputFieldNameDefault = outputFieldNameDefault;
      this.tableJson = tableJson;
      this.forUpdate = forUpdate;
      this.typesFileHeader = typesFileHeader;
      if ( valueOr(generateResultTypes, true) && !valueOr(generateSource, true) )
         throw new RuntimeException(
            "In query \"" + queryName + "\", cannot generate result types without " +
            "generateSource option enabled."
         );
   }

   public String getQueryName() { return queryName; }

   /// Generates a SQL query for each of the specified result representations.
   public @Nullable List<ResultsRepr> getResultsRepresentations() { return resultsRepresentations; }

   @JsonIgnore
   public List<ResultsRepr> getResultsRepresentationsList()
   {
      return resultsRepresentations != null ? resultsRepresentations: singletonList(JSON_OBJECT_ROWS);
   }

   public @Nullable Boolean getGenerateResultTypes() { return generateResultTypes; }

   @JsonIgnore
   public boolean getGenerateResultTypesOrDeault()
   {
      return generateResultTypes != null ? generateResultTypes: true;
   }

   public @Nullable Boolean getGenerateSource() { return generateSource; }

   @JsonIgnore
   public boolean getGenerateSourceOrDefault()
   {
      return generateSource != null ? generateSource: true;
   }

   public @Nullable OutputFieldNameDefault getOutputFieldNameDefault() { return outputFieldNameDefault; }

   public TableJsonSpec getTableJson() { return tableJson; }

   public @Nullable Boolean getForUpdate() { return forUpdate; }

   @JsonIgnore
   public boolean getForUpdateOrDefault()
   {
      return forUpdate != null ? forUpdate : false;
   }

   public @Nullable String getTypesFileHeader() { return typesFileHeader; }
}
