package org.sqljson.specs;

import java.util.Collections;
import java.util.List;


public class RecordCondition
{
    private String sql;
    private List<String> paramNames = Collections.emptyList();

    private RecordCondition()
   {
      this.sql =  "";
   }

    public RecordCondition(String sql, List<String> paramNames)
    {
        this.sql = sql;
        this.paramNames = paramNames;
    }

    public String getSql() { return sql; }

    public List<String> getParamNames() { return paramNames; }
}
