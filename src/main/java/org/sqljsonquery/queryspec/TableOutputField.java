package org.sqljsonquery.queryspec;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;


public final class TableOutputField
{
   private String databaseFieldName;
   private Optional<String> outputName = Optional.empty();
   private List<FieldTypeOverride> fieldTypeOverrides = emptyList();

   protected TableOutputField() {}

   public TableOutputField
   (
      String databaseFieldName,
      Optional<String> outputName,
      List<FieldTypeOverride> fieldTypeOverrides
   )
   {
      this.databaseFieldName = requireNonNull(databaseFieldName, "field name");
      this.outputName = requireNonNull(outputName, "output element name");
      this.fieldTypeOverrides = unmodifiableList(new ArrayList<>(fieldTypeOverrides));
   }

   public String getDatabaseFieldName() { return databaseFieldName; }

   public Optional<String> getOutputName() { return outputName; }

   public List<FieldTypeOverride> getFieldTypeOverrides() { return fieldTypeOverrides; }
}
