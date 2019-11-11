package org.sqljson;

import java.util.*;
import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableMap;
import static java.util.Objects.requireNonNull;

import org.sqljson.result_types.GeneratedType;
import org.sqljson.specs.queries.ResultsRepr;


public class GeneratedQuery
{
   private final String queryName;
   private final Map<ResultsRepr,String> generatedSqls;
   private final List<GeneratedType> generatedResultTypes;
   private final boolean generateSourceEnabled;

   public GeneratedQuery
   (
      String queryName,
      Map<ResultsRepr,String> generatedSqls,
      List<GeneratedType> generatedResultTypes,
      boolean generateSourceEnabled
   )
   {
      this.queryName = queryName;
      this.generatedSqls = unmodifiableMap(new HashMap<>(generatedSqls));
      this.generatedResultTypes = unmodifiableList(new ArrayList<>(generatedResultTypes));
      this.generateSourceEnabled = generateSourceEnabled;
   }

   public String getName() { return queryName; }

   public Set<ResultsRepr> getResultRepresentations() { return new HashSet<>(generatedSqls.keySet()); }

   public String getSql(ResultsRepr resultsRepr) { return requireNonNull(generatedSqls.get(resultsRepr)); }

   public Map<ResultsRepr,String> getGeneratedSqls() { return generatedSqls; }

   public List<GeneratedType> getGeneratedResultTypes() { return generatedResultTypes; }

   public boolean getGenerateSourceEnabled() { return generateSourceEnabled; }
}
