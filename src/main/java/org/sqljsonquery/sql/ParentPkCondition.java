package org.sqljsonquery.sql;

import java.util.ArrayList;
import java.util.List;
import static java.util.Collections.unmodifiableList;
import static java.util.stream.Collectors.joining;

import gov.fda.nctr.dbmd.DBMD;
import gov.fda.nctr.dbmd.ForeignKey;


public class ParentPkCondition implements ParentChildCondition
{
   private final String childAlias;
   private final List<ForeignKey.Component> matchedFields;

   public ParentPkCondition(String childAlias, List<ForeignKey.Component> matchedFields)
   {
      this.childAlias = childAlias;
      this.matchedFields = unmodifiableList(new ArrayList<>(matchedFields));
   }

   public String getOtherTableAlias() { return childAlias; }

   public List<ForeignKey.Component> getMatchedFields() { return matchedFields; }

   public String asEquationConditionOn(String parentAlias, DBMD dbmd)
   {
      return
         matchedFields.stream()
         .map(mf -> childAlias + "." + dbmd.quoteIfNeeded(mf.getForeignKeyFieldName()) + " = " +
                    parentAlias + "." + dbmd.quoteIfNeeded(mf.getPrimaryKeyFieldName()))
         .collect(joining(" and "));
   }
}
