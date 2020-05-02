package org.sqljson.source_writers;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.sqljson.GeneratedModStatement;
import org.sqljson.GeneratedQuery;
import org.sqljson.WrittenQueryReprPath;


public interface SourceCodeWriter
{
   void writeQueries
      (
         List<GeneratedQuery> generatedQueries,
         List<WrittenQueryReprPath> writtenPaths,
         boolean includeTimestamp
      )
      throws IOException;

   void writeModStatements
      (
         List<GeneratedModStatement> generatedModStatements,
         Map<String,Path> writtenPathsByModName,
         boolean includeTimestamp
      )
      throws IOException;
}

