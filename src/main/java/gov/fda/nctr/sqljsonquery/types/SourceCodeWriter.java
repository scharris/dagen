package gov.fda.nctr.sqljsonquery.types;

import java.io.IOException;
import java.util.List;

import gov.fda.nctr.sqljsonquery.SqlJsonQuery;


public interface SourceCodeWriter
{
   void writeSourceCode(List<SqlJsonQuery> generatedQueries) throws IOException;
}
