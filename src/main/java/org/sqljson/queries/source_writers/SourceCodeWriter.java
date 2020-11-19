package org.sqljson.queries.source_writers;

import java.io.IOException;
import java.util.List;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.sqljson.queries.QueryReprSqlPath;
import org.sqljson.queries.result_types.ResultType;


public interface SourceCodeWriter
{
   void writeQuerySourceCode
      (
         String queryName,
         List<ResultType> resultTypes,
         List<String> paramNames,
         List<QueryReprSqlPath> sqlPaths,
         @Nullable String queryFileHeader,
         boolean includeTimestamp
      )
      throws IOException;
}

