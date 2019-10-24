package gov.fda.nctr.sqljsonquery;

import java.util.*;
import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableMap;
import static java.util.Objects.requireNonNull;

import gov.fda.nctr.sqljsonquery.types.GeneratedType;
import gov.fda.nctr.sqljsonquery.spec.ResultsRepr;


public class SqlJsonQuery
{
   private final String queryName;
   private final Map<ResultsRepr,String> generatedSqls;
   private final List<GeneratedType> generatedResultTypes;

   public SqlJsonQuery
   (
      String queryName,
      Map<ResultsRepr,String> generatedSqls,
      List<GeneratedType> generatedResultTypes
   )
   {
      this.queryName = queryName;
      this.generatedSqls = unmodifiableMap(new HashMap<>(generatedSqls));
      this.generatedResultTypes = unmodifiableList(new ArrayList<>(generatedResultTypes));
   }

   public String getQueryName() { return queryName; }

   public Set<ResultsRepr> getResultRepresentations() { return new HashSet<>(generatedSqls.keySet()); }

   public String getSql(ResultsRepr resultsRepr) { return requireNonNull(generatedSqls.get(resultsRepr)); }

   public Map<ResultsRepr,String> getGeneratedSqls() { return generatedSqls; }

   public List<GeneratedType> getGeneratedResultTypes() { return generatedResultTypes; }
}
