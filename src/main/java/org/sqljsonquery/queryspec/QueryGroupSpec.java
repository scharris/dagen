package org.sqljsonquery.queryspec;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;


public final class QueryGroupSpec
{
   private Optional<String> defaultSchema = Optional.empty();
   private List<QuerySpec> querySpecs = emptyList();

   protected QueryGroupSpec() {}

   public QueryGroupSpec
   (
      Optional<String> defaultSchema,
      List<QuerySpec> querySpecs
   )
   {
      this.defaultSchema = defaultSchema;
      this.querySpecs = unmodifiableList(new ArrayList<>(querySpecs));
   }

   public Optional<String> getDefaultSchema() { return defaultSchema; }

   public List<QuerySpec> getQuerySpecs() { return querySpecs; }
}
