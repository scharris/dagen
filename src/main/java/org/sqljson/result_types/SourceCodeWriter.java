package org.sqljson.result_types;

import java.io.IOException;
import java.util.List;

import org.sqljson.GeneratedQuery;
import org.sqljson.WrittenQueryReprPath;


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
