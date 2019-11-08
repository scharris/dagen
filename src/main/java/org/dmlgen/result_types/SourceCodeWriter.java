package org.dmlgen.result_types;

import java.io.IOException;
import java.util.List;

import org.dmlgen.GeneratedQuery;
import org.dmlgen.WrittenQueryReprPath;


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
