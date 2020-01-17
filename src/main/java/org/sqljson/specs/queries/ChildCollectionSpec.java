package org.sqljson.specs.queries;

import java.util.*;

import com.fasterxml.jackson.annotation.JsonIgnore;


public final class ChildCollectionSpec
{
   private String collectionName;
   private TableJsonSpec tableJson;
   private Optional<List<String>> foreignKeyFields = Optional.empty();
   private Optional<String> filter = Optional.empty();

   private ChildCollectionSpec() {}

   public ChildCollectionSpec
   (
      String collectionName,
      TableJsonSpec tableJson,
      Optional<List<String>> fkFields,
      Optional<String> filter
   )
   {
      this.collectionName = collectionName;
      this.tableJson = tableJson;
      this.foreignKeyFields = fkFields.map(Collections::unmodifiableList);
      this.filter = filter;
   }

   public String getCollectionName() { return collectionName; }

   public TableJsonSpec getTableJson() { return tableJson; }

   public Optional<List<String>> getForeignKeyFields() { return foreignKeyFields; }

   @JsonIgnore
   public Optional<Set<String>> getForeignKeyFieldsSet() { return foreignKeyFields.map(HashSet::new); }

   public Optional<String> getFilter() { return filter; }
}
