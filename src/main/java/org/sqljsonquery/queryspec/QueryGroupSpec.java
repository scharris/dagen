package org.sqljsonquery.queryspec;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;
import static java.util.Optional.empty;
import static java.util.function.Function.identity;

import com.fasterxml.jackson.annotation.JsonIgnore;

import org.sqljsonquery.util.StringFuns;
import static org.sqljsonquery.queryspec.OutputFieldNameDefault.CAMELCASE;


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


   @JsonIgnore
   public Function<String,String> getDefaultFieldOutputNameFunction()
   {
      switch ( outputFieldNameDefault )
      {
         case AS_IN_DB: return identity();
         case CAMELCASE: return StringFuns::lowerCamelCase;
         default: throw new RuntimeException("Unexpected output field name default.");
      }
   }
}
