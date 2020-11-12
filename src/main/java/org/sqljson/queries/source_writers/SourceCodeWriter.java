package org.sqljson.queries.source_writers;

import java.io.IOException;
import java.util.List;

import org.sqljson.queries.GeneratedQuery;
import org.sqljson.queries.QueryReprSqlPath;


public interface SourceCodeWriter
{
   void writeQueries
      (
         List<GeneratedQuery> generatedQueries,
         List<QueryReprSqlPath> writtenPaths,
         boolean includeTimestamp
      )
      throws IOException;
}

