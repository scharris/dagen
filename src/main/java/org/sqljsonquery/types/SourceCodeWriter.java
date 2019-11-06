package org.sqljsonquery.types;

import java.io.IOException;
import java.util.List;

import org.sqljsonquery.SqlJsonQuery;
import org.sqljsonquery.WrittenQueryReprPath;


public interface SourceCodeWriter
{
   void writeSourceCode(List<SqlJsonQuery> generatedQueries, List<WrittenQueryReprPath> writtenPaths, boolean includeTimestamp) throws IOException;
}
