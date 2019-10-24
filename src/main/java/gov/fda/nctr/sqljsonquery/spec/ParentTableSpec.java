package gov.fda.nctr.sqljsonquery.spec;

import java.util.*;

import com.fasterxml.jackson.annotation.JsonIgnore;


public final class ParentTableSpec
{
   private Optional<String> wrapperFieldName = Optional.empty();
   private TableOutputSpec tableOutputSpec;
   private Optional<List<String>> childForeignKeyFields = Optional.empty();
   private boolean omitChildRowWhenUnwrappedParentMissing = false;

   protected ParentTableSpec() {}

   public ParentTableSpec
   (
      Optional<String> fieldName,
      TableOutputSpec tableOutputSpec,
      Optional<List<String>> childForeignKeyFields,
      boolean omitChildRowWhenUnwrappedParentMissing
   )
   {
      this.wrapperFieldName = fieldName;
      this.tableOutputSpec = tableOutputSpec;
      this.childForeignKeyFields = childForeignKeyFields.map(Collections::unmodifiableList);
      this.omitChildRowWhenUnwrappedParentMissing = omitChildRowWhenUnwrappedParentMissing;
   }

   public Optional<String> getWrapperFieldName() { return wrapperFieldName; }

   public TableOutputSpec getTableOutputSpec() { return tableOutputSpec; }

   public Optional<List<String>> getChildForeignKeyFields() { return childForeignKeyFields; }

   public boolean getOmitChildRowWhenUnwrappedParentMissing() { return omitChildRowWhenUnwrappedParentMissing; }

   @JsonIgnore
   public Optional<Set<String>> getChildForeignKeyFieldsSet() { return childForeignKeyFields.map(HashSet::new); }
}
