package org.sqljsonquery.spec;

import java.util.*;

import com.fasterxml.jackson.annotation.JsonIgnore;


public final class ChildTableSpec
{
   private String collectionFieldName;
   private TableOutputSpec tableOutputSpec;
   private Optional<List<String>> foreignKeyFields = Optional.empty();
   private Optional<String> filter = Optional.empty();

   protected ChildTableSpec() {}

   public ChildTableSpec
   (
      String fieldName,
      TableOutputSpec tableOutputSpec,
      Optional<List<String>> fkFields,
      Optional<String> filter
   )
   {
      this.collectionFieldName = fieldName;
      this.tableOutputSpec = tableOutputSpec;
      this.foreignKeyFields = fkFields.map(Collections::unmodifiableList);
      this.filter = filter;
   }

   public String getCollectionFieldName() { return collectionFieldName; }

   public TableOutputSpec getTableOutputSpec() { return tableOutputSpec; }

   public Optional<List<String>> getForeignKeyFields() { return foreignKeyFields; }

   @JsonIgnore
   public Optional<Set<String>> getForeignKeyFieldsSet() { return foreignKeyFields.map(HashSet::new); }

   public Optional<String> getFilter() { return filter; }
}
