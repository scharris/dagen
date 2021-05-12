package org.sqljson;

import java.nio.file.Path;

import org.sqljson.query_specs.ResultRepr;


public class QueryReprSqlPath
{
   private final String queryName;
   private final ResultRepr resultRepr;
   private final Path sqlPath;

   public QueryReprSqlPath
      (
         String queryName,
         ResultRepr resultRepr,
         Path sqlPath
      )
   {
      this.queryName = queryName;
      this.resultRepr = resultRepr;
      this.sqlPath = sqlPath;
   }

   public String getQueryName() { return queryName; }

   public ResultRepr getResultRepr() { return resultRepr; }

   public Path getSqlPath() { return sqlPath; }
}

