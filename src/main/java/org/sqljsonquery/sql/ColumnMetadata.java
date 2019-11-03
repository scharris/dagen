package org.sqljsonquery.sql;

public class ColumnMetadata
{
   private final String outputName;
   private final SelectClauseEntry.Source source;

   public ColumnMetadata(String outputName, SelectClauseEntry.Source source)
   {
      this.outputName = outputName;
      this.source = source;
   }
   public String getOutputName() { return outputName; }

   public SelectClauseEntry.Source getSource() { return source; }
}
