package io.sqljson.result_types;

import java.io.IOException;
import java.util.List;

import io.sqljson.GeneratedQuery;
import io.sqljson.WrittenQueryReprPath;


public interface SourceCodeWriter
{
   void writeSourceCode
   (
      List<GeneratedQuery> generatedQueries,
      List<WrittenQueryReprPath> writtenPaths,
      boolean includeTimestamp
   )
      throws IOException;
}
