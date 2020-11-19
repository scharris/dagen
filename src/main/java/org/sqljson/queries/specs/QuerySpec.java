package org.sqljson.queries.specs;

import java.util.ArrayList;
import java.util.List;
import static java.util.Collections.*;

import org.checkerframework.checker.nullness.qual.Nullable;

import com.fasterxml.jackson.annotation.JsonIgnore;

import static org.sqljson.util.Nullables.valueOr;
import static org.sqljson.queries.specs.ResultRepr.JSON_OBJECT_ROWS;


public final class QuerySpec
{
   private final String queryName;
   private final TableJsonSpec tableJson;
   private final @Nullable List<ResultRepr> resultRepresentations;
   private final @Nullable Boolean generateResultTypes;
   private final @Nullable Boolean generateSource; // Contains at least the resource name for generated SQL, if not result types.
   private final @Nullable OutputFieldNameDefault outputFieldNameDefault; // inherited from query group spec if empty
   private final @Nullable String orderBy;
   private final @Nullable Boolean forUpdate;
   private final @Nullable String typesFileHeader;

   private QuerySpec()
   {
      this.queryName = "";
      this.tableJson = new TableJsonSpec();
      this.resultRepresentations = singletonList(JSON_OBJECT_ROWS);
      this.generateResultTypes = true;
      this.generateSource = true;
      this.outputFieldNameDefault = null;
      this.orderBy = null;
      this.forUpdate = false;
      this.typesFileHeader = null;
   }

   public QuerySpec
      (
         String queryName,
         TableJsonSpec tableJson,
         @Nullable List<ResultRepr> resultRepresentations,
         @Nullable Boolean generateResultTypes,
         @Nullable Boolean generateSource,
         @Nullable OutputFieldNameDefault outputFieldNameDefault,
         @Nullable String orderBy,
         @Nullable Boolean forUpdate,
         @Nullable String typesFileHeader
      )
   {
      this.queryName = queryName;
      this.resultRepresentations = resultRepresentations != null ?
         unmodifiableList(new ArrayList<>(resultRepresentations))
         : singletonList(JSON_OBJECT_ROWS);
      this.generateResultTypes = generateResultTypes;
      this.generateSource = generateSource;
      this.outputFieldNameDefault = outputFieldNameDefault;
      this.tableJson = tableJson;
      this.orderBy = orderBy;
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
   public @Nullable List<ResultRepr> getResultRepresentations() { return resultRepresentations; }

   @JsonIgnore
   public List<ResultRepr> getResultRepresentationsList()
   {
      return resultRepresentations != null ? resultRepresentations : singletonList(JSON_OBJECT_ROWS);
   }

   public @Nullable Boolean getGenerateResultTypes() { return generateResultTypes; }

   @JsonIgnore
   public boolean getGenerateResultTypesOrDefault()
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

   public @Nullable String getOrderBy() { return orderBy; }

   public @Nullable Boolean getForUpdate() { return forUpdate; }

   @JsonIgnore
   public boolean getForUpdateOrDefault()
   {
      return forUpdate != null ? forUpdate : false;
   }

   public @Nullable String getTypesFileHeader() { return typesFileHeader; }
}
