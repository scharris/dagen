package org.sqljson.queries.sql;

import org.sqljson.dbmd.DatabaseMetadata;


/// Represents some condition on a table in context with reference to another
/// table which is identified by its alias.
public interface ParentChildCondition
{
   String asEquationConditionOn
      (
         String tableAlias,
         DatabaseMetadata dbmd
      );

   String getOtherTableAlias();
}
