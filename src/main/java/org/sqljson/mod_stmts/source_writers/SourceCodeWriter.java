package org.sqljson.mod_stmts.source_writers;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.sqljson.mod_stmts.GeneratedModStatement;


public interface SourceCodeWriter
{
   void writeModStatements
      (
         List<GeneratedModStatement> generatedModStatements,
         Map<String,Path> writtenPathsByModName,
         boolean includeTimestamp
      )
      throws IOException;
}

