package org.sqljson.queries.source_writers;

import java.io.IOException;
import java.util.List;

import org.sqljson.queries.GeneratedQuery;
import org.sqljson.queries.WrittenQueryReprPath;


public interface SourceCodeWriter
{
   void writeQueries
      (
         List<GeneratedQuery> generatedQueries,
         List<WrittenQueryReprPath> writtenPaths,
         boolean includeTimestamp
      )
      throws IOException;
}

