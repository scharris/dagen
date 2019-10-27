package org.sqljsonquery.queryspec;

import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonIgnore;

import static java.util.Objects.requireNonNull;


public final class TableOutputField
{
   private String databaseFieldName;
   private Optional<String> outputName = Optional.empty();

   protected TableOutputField() {}

   public TableOutputField(String databaseFieldName, Optional<String> outputName)
   {
      this.databaseFieldName = requireNonNull(databaseFieldName, "field name");
      this.outputName = requireNonNull(outputName, "output element name");
   }

   public String getDatabaseFieldName() { return databaseFieldName; }

   public Optional<String> getOutputName() { return outputName; }

   @JsonIgnore
   public String getFinalOutputFieldName()
   {
      return outputName.orElse(databaseFieldName);
   }
}
