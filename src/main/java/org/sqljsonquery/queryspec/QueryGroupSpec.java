package org.sqljsonquery.queryspec;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import org.sqljsonquery.util.StringFuns;
import com.fasterxml.jackson.annotation.JsonIgnore;

import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;

import static java.util.function.Function.identity;
import static org.sqljsonquery.queryspec.OutputFieldNameDefault.CAMELCASE;


public final class QueryGroupSpec
{
   private Optional<String> defaultSchema = Optional.empty();
   private OutputFieldNameDefault outputFieldNameDefault = CAMELCASE;
   private List<QuerySpec> querySpecs = emptyList();

   protected QueryGroupSpec() {}

   public QueryGroupSpec
   (
      Optional<String> defaultSchema,
      OutputFieldNameDefault outputFieldNameDefault,
      List<QuerySpec> querySpecs
   )
   {
      this.defaultSchema = defaultSchema;
      this.querySpecs = unmodifiableList(new ArrayList<>(querySpecs));
      this.outputFieldNameDefault = outputFieldNameDefault;
   }

   public Optional<String> getDefaultSchema() { return defaultSchema; }

   public List<QuerySpec> getQuerySpecs() { return querySpecs; }

   public OutputFieldNameDefault getOutputFieldNameDefault() { return outputFieldNameDefault; }

   @JsonIgnore
   public Function<String,String> getDefaultOutputNameFunction()
   {
      switch ( outputFieldNameDefault )
      {
         case AS_IN_DB: return identity();
         case CAMELCASE: return StringFuns::lowerCamelCase;
         default: throw new RuntimeException("Unexpected output field name default.");
      }
   }
}
