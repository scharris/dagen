package org.sqljsonquery.spec;

import java.util.*;

import com.fasterxml.jackson.annotation.JsonIgnore;


public final class InlineParentTableSpec implements ParentTableSpec
{
   private TableOutputSpec inlineParentTableOutputSpec;
   private Optional<List<String>> childForeignKeyFields = Optional.empty();

   protected InlineParentTableSpec() {}

   public InlineParentTableSpec
   (
      TableOutputSpec inlineParentTableOutputSpec,
      Optional<List<String>> childForeignKeyFields
   )
   {
      this.inlineParentTableOutputSpec = inlineParentTableOutputSpec;
      this.childForeignKeyFields = childForeignKeyFields.map(Collections::unmodifiableList);
   }

   public TableOutputSpec getInlineParentTableOutputSpec() { return inlineParentTableOutputSpec; }

   public Optional<List<String>> getChildForeignKeyFields() { return childForeignKeyFields; }

   @JsonIgnore
   public TableOutputSpec getParentTableOutputSpec() { return inlineParentTableOutputSpec; }

   @JsonIgnore
   public Optional<Set<String>> getChildForeignKeyFieldsSet() { return childForeignKeyFields.map(HashSet::new); }
}
