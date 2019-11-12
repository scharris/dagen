package org.sqljson.specs.queries;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;
import static java.util.Optional.empty;

import static org.sqljson.specs.queries.OutputFieldNameDefault.CAMELCASE;


public final class QueryGroupSpec
{
   private Optional<String> defaultSchema = empty();
   private OutputFieldNameDefault outputFieldNameDefault = CAMELCASE;
   private List<String> generateUnqualifiedNamesForSchemas = emptyList();
   private List<QuerySpec> querySpecs = emptyList();

   private QueryGroupSpec() {}

   public QueryGroupSpec
   (
      Optional<String> defaultSchema,
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

   public Optional<String> getDefaultSchema() { return defaultSchema; }

   public OutputFieldNameDefault getOutputFieldNameDefault() { return outputFieldNameDefault; }

   public List<String> getGenerateUnqualifiedNamesForSchemas() { return generateUnqualifiedNamesForSchemas; }

   public List<QuerySpec> getQuerySpecs() { return querySpecs; }
}
