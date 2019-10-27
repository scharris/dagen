package org.sqljsonquery.queryspec;

import java.util.*;

import com.fasterxml.jackson.annotation.JsonIgnore;


public final class ReferencedParentSpec implements ParentSpec
{
   private String referenceFieldName;
   private TableOutputSpec referencedParentTableOutputSpec;
   private Optional<List<String>> childForeignKeyFields = Optional.empty();

   protected ReferencedParentSpec() {}

   public ReferencedParentSpec
   (
      String fieldName,
      TableOutputSpec referencedParentTableOutputSpec,
      Optional<List<String>> childForeignKeyFields
   )
   {
      this.referenceFieldName = fieldName;
      this.referencedParentTableOutputSpec = referencedParentTableOutputSpec;
      this.childForeignKeyFields = childForeignKeyFields.map(Collections::unmodifiableList);
   }

   public String getReferenceFieldName() { return referenceFieldName; }

   public TableOutputSpec getReferencedParentTableOutputSpec() { return referencedParentTableOutputSpec; }

   public Optional<List<String>> getChildForeignKeyFields() { return childForeignKeyFields; }

   @JsonIgnore
   public TableOutputSpec getParentTableOutputSpec() { return referencedParentTableOutputSpec; }

   @JsonIgnore
   public Optional<Set<String>> getChildForeignKeyFieldsSet() { return childForeignKeyFields.map(HashSet::new); }
}
