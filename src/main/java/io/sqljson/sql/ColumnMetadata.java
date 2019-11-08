package io.sqljson.sql;

import java.util.ArrayList;
import java.util.List;
import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;

import io.sqljson.specs.queries.FieldTypeOverride;


public class ColumnMetadata
{
   private final String outputName;
   private final SelectClauseEntry.Source source;
   private final List<FieldTypeOverride> fieldTypeOverrides;

   public ColumnMetadata(String outputName, SelectClauseEntry.Source source)
   {
      this(outputName, source, emptyList());
   }

   public ColumnMetadata
   (
      String outputName,
      SelectClauseEntry.Source source,
      List<FieldTypeOverride> fieldTypeOverrides
   )
   {
      this.outputName = outputName;
      this.source = source;
      this.fieldTypeOverrides = unmodifiableList(new ArrayList<>(fieldTypeOverrides));
   }

   public String getOutputName() { return outputName; }

   public SelectClauseEntry.Source getSource() { return source; }

   public List<FieldTypeOverride> getFieldTypeOverrides() { return fieldTypeOverrides; }
}
