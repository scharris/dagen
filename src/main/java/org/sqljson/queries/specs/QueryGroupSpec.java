package org.sqljson.queries.specs;

import java.util.ArrayList;
import java.util.List;
import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;

import org.checkerframework.checker.nullness.qual.Nullable;

import static org.sqljson.queries.specs.OutputFieldNameDefault.CAMELCASE;


public final class QueryGroupSpec
{
   private @Nullable String defaultSchema = null;
   private OutputFieldNameDefault outputFieldNameDefault = CAMELCASE;
   private List<String> generateUnqualifiedNamesForSchemas = emptyList();
   private List<QuerySpec> querySpecs = emptyList();

   private QueryGroupSpec() {}

   public QueryGroupSpec
      (
         @Nullable String defaultSchema,
         OutputFieldNameDefault outputFieldNameDefault,
         List<String> generateUnqualifiedNamesForSchemas,
         List<QuerySpec> querySpecs
      )
   {
      this.defaultSchema = defaultSchema;
      this.outputFieldNameDefault = outputFieldNameDefault;
      this.generateUnqualifiedNamesForSchemas = generateUnqualifiedNamesForSchemas;
      this.querySpecs = unmodifiableList(new ArrayList<>(querySpecs));
   }

   public @Nullable String getDefaultSchema() { return defaultSchema; }

   public OutputFieldNameDefault getOutputFieldNameDefault() { return outputFieldNameDefault; }

   public List<String> getGenerateUnqualifiedNamesForSchemas() { return generateUnqualifiedNamesForSchemas; }

   public List<QuerySpec> getQuerySpecs() { return querySpecs; }
}

