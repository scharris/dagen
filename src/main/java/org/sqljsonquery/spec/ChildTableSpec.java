package org.sqljsonquery.spec;

import java.util.*;

import com.fasterxml.jackson.annotation.JsonIgnore;


public final class ChildTableSpec
{
   private String childCollectionName;
   private TableOutputSpec childTableOutputSpec;
   private Optional<List<String>> foreignKeyFields = Optional.empty();
   private Optional<String> filter = Optional.empty();

   protected ChildTableSpec() {}

   public ChildTableSpec
   (
      String fieldName,
      TableOutputSpec childTableOutputSpec,
      Optional<List<String>> fkFields,
      Optional<String> filter
   )
   {
      this.childCollectionName = fieldName;
      this.childTableOutputSpec = childTableOutputSpec;
      this.foreignKeyFields = fkFields.map(Collections::unmodifiableList);
      this.filter = filter;
   }

   public String getChildCollectionName() { return childCollectionName; }

   public TableOutputSpec getChildTableOutputSpec() { return childTableOutputSpec; }

   public Optional<List<String>> getForeignKeyFields() { return foreignKeyFields; }

   @JsonIgnore
   public Optional<Set<String>> getForeignKeyFieldsSet() { return foreignKeyFields.map(HashSet::new); }

   public Optional<String> getFilter() { return filter; }
}
