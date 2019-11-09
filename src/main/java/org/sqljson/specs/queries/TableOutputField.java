package org.sqljson.specs.queries;

import java.util.List;
import java.util.Optional;
import static java.util.Collections.emptyList;

import com.fasterxml.jackson.annotation.JsonIgnore;

import org.sqljson.util.StringFuns;


public final class TableOutputField
{
   private Optional<String> databaseFieldName = Optional.empty();
   private Optional<String> fieldExpression = Optional.empty();
   private Optional<String> outputName = Optional.empty();
   private List<FieldTypeOverride> fieldTypeOverrides = emptyList();

   private TableOutputField() {}

   public TableOutputField
   (
      Optional<String> databaseFieldName,
      Optional<String> fieldExpression,
      Optional<String> outputName,
      List<FieldTypeOverride> fieldTypeOverrides
   )
   {
      this.databaseFieldName = databaseFieldName;
      this.fieldExpression = fieldExpression;
      this.outputName = outputName;
      this.fieldTypeOverrides = fieldTypeOverrides;
   }

   public String getDatabaseFieldName() { return databaseFieldName.get(); }

   public String getFieldExpression() { return fieldExpression.get(); }

   public Optional<String> getOutputName() { return outputName; }

   public List<FieldTypeOverride> getFieldTypeOverrides() { return fieldTypeOverrides; }

   public Optional<FieldTypeOverride> getTypeOverride(String language)
   {
      return fieldTypeOverrides.stream().filter(to -> to.getLanguage().equals(language)).findAny();
   }

   @JsonIgnore
   public boolean isSimpleField()
   {
      if ( databaseFieldName.isPresent() == fieldExpression.isPresent() )
         throw new RuntimeException("Exactly one of database field name and value expression should be specified.");

      return databaseFieldName.isPresent();
   }

   public String getValueExpressionForAlias(String tableAlias)
   {
      if ( isSimpleField() )
         return tableAlias + "." + databaseFieldName.get();
      else
         return StringFuns.substituteVarValue(fieldExpression.get(), tableAlias);
   }
}
