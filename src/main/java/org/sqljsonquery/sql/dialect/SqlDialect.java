package org.sqljsonquery.sql.dialect;

import java.util.List;


public interface SqlDialect
{
   String getJsonObjectSelectExpression(List<String> fromColumnNames, String fromAlias);

   String getJsonAggregatedObjectsSelectExpression(List<String> fromColumnNames, String fromAlias);
}
