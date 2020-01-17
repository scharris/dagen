package org.sqljson.specs.queries;

import java.util.*;

import com.fasterxml.jackson.annotation.JsonIgnore;


public final class ReferencedParentSpec implements ParentSpec
{
   private String referenceName;
   private TableJsonSpec tableJson;
   private Optional<List<String>> viaForeignKeyFields = Optional.empty();

   private ReferencedParentSpec() {}

   public ReferencedParentSpec
   (
      String referenceName,
      TableJsonSpec tableJson,
      Optional<List<String>> viaForeignKeyFields
   )
   {
      this.referenceName = referenceName;
      this.tableJson = tableJson;
      this.viaForeignKeyFields = viaForeignKeyFields.map(Collections::unmodifiableList);
   }

   public String getReferenceName() { return referenceName; }

   public TableJsonSpec getTableJson() { return getParentTableJsonSpec(); }

   public Optional<List<String>> getViaForeignKeyFields() { return viaForeignKeyFields; }

   @JsonIgnore
   public TableJsonSpec getParentTableJsonSpec() { return tableJson; }

   @JsonIgnore
   public Optional<Set<String>> getChildForeignKeyFieldsSet() { return viaForeignKeyFields.map(HashSet::new); }
}
