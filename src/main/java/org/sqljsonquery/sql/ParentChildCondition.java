package org.sqljsonquery.sql;

import gov.fda.nctr.dbmd.DBMD;

/// Represents some condition on a table in context with reference to another
/// table which is identified by its alias.
public interface ParentChildCondition
{
   String asEquationConditionOn(String tableAlias, DBMD dbmd);

   String getOtherTableAlias();
}
