package org.sqljsonquery.spec;

import java.util.*;

import com.fasterxml.jackson.annotation.JsonIgnore;


public final class WrappedParentTableSpec implements ParentTableSpec
{
   private String wrapperFieldName;
   private TableOutputSpec wrappedParentTableOutputSpec;
   private Optional<List<String>> childForeignKeyFields = Optional.empty();

   protected WrappedParentTableSpec() {}

   public WrappedParentTableSpec
   (
      String fieldName,
      TableOutputSpec wrappedParentTableOutputSpec,
      Optional<List<String>> childForeignKeyFields
   )
   {
      this.wrapperFieldName = fieldName;
      this.wrappedParentTableOutputSpec = wrappedParentTableOutputSpec;
      this.childForeignKeyFields = childForeignKeyFields.map(Collections::unmodifiableList);
   }

   public String getWrapperFieldName() { return wrapperFieldName; }

   public TableOutputSpec getWrappedParentTableOutputSpec() { return wrappedParentTableOutputSpec; }

   public Optional<List<String>> getChildForeignKeyFields() { return childForeignKeyFields; }

   @JsonIgnore
   public TableOutputSpec getParentTableOutputSpec() { return wrappedParentTableOutputSpec; }

   @JsonIgnore
   public Optional<Set<String>> getChildForeignKeyFieldsSet() { return childForeignKeyFields.map(HashSet::new); }
}
