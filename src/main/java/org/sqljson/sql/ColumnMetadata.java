package org.sqljson.sql;

import java.util.ArrayList;
import java.util.List;
import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;

import org.sqljson.specs.queries.FieldTypeOverride;


public class ColumnMetadata
{
   private final String name;
   private final SelectClauseEntry.Source source;
   private final List<FieldTypeOverride> fieldTypeOverrides;

   public ColumnMetadata(String name, SelectClauseEntry.Source source)
   {
      this(name, source, emptyList());
   }

   public ColumnMetadata
   (
      String name,
      SelectClauseEntry.Source source,
      List<FieldTypeOverride> fieldTypeOverrides
   )
   {
      this.name = name;
      this.source = source;
      this.fieldTypeOverrides = unmodifiableList(new ArrayList<>(fieldTypeOverrides));
   }

   public String getName() { return name; }

   public SelectClauseEntry.Source getSource() { return source; }

   public List<FieldTypeOverride> getFieldTypeOverrides() { return fieldTypeOverrides; }
}
