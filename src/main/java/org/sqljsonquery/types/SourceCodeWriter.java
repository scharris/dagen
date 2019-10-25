package org.sqljsonquery.types;

import java.io.IOException;
import java.util.List;

import org.sqljsonquery.SqlJsonQuery;


public interface SourceCodeWriter
{
   void writeSourceCode(List<SqlJsonQuery> generatedQueries) throws IOException;
}
