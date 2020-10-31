package org.sqljson.dbmd.source_writers;

import java.io.IOException;

import org.sqljson.dbmd.DatabaseMetadata;


public interface SourceCodeWriter
{
   void writeRelationDefinitions
      (
         DatabaseMetadata dbmd,
         boolean includeTimestamp
      )
      throws IOException;
}

