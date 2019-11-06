package org.sqljsonquery.sql;

import java.util.ArrayList;
import java.util.List;
import static java.util.stream.Collectors.*;
import static java.util.Collections.unmodifiableList;

import org.sqljsonquery.dbmd.DatabaseMetadata;
import org.sqljsonquery.dbmd.ForeignKey;


public class ChildFkCondition implements ParentChildCondition
{
   private final String parentAlias;
   private final List<ForeignKey.Component> matchedFields;

   public ChildFkCondition(String parentAlias, List<ForeignKey.Component> matchedFields)
   {
      this.parentAlias = parentAlias;
      this.matchedFields = unmodifiableList(new ArrayList<>(matchedFields));
   }

   public String getOtherTableAlias() { return parentAlias; }

   public List<ForeignKey.Component> getMatchedFields() { return matchedFields; }

   public String asEquationConditionOn(String childAlias, DatabaseMetadata dbmd)
   {
      return
         matchedFields.stream()
         .map(mf -> childAlias + "." + dbmd.quoteIfNeeded(mf.getForeignKeyFieldName()) + " = " +
                    parentAlias + "." + dbmd.quoteIfNeeded(mf.getPrimaryKeyFieldName()))
         .collect(joining(" and "));
   }
}
