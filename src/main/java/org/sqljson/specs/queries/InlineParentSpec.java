package org.sqljson.specs.queries;

import java.util.*;

import com.fasterxml.jackson.annotation.JsonIgnore;


public final class InlineParentSpec implements ParentSpec
{
   private TableJsonSpec tableJson;
   private Optional<List<String>> viaForeignKeyFields = Optional.empty();

   private InlineParentSpec() {}

   public InlineParentSpec
   (
      TableJsonSpec tableJson,
      Optional<List<String>> viaForeignKeyFields
   )
   {
      this.tableJson = tableJson;
      this.viaForeignKeyFields = viaForeignKeyFields.map(Collections::unmodifiableList);
   }

   public TableJsonSpec getTableJson() { return getParentTableJsonSpec(); }

   public Optional<List<String>> getViaForeignKeyFields() { return viaForeignKeyFields; }

   @JsonIgnore
   public TableJsonSpec getParentTableJsonSpec() { return tableJson; }

   @JsonIgnore
   public Optional<Set<String>> getChildForeignKeyFieldsSet() { return viaForeignKeyFields.map(HashSet::new); }
}
