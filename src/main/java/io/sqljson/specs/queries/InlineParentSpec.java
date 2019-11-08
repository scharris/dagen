package io.sqljson.specs.queries;

import java.util.*;

import com.fasterxml.jackson.annotation.JsonIgnore;


public final class InlineParentSpec implements ParentSpec
{
   private TableOutputSpec inlineParentTableOutputSpec;
   private Optional<List<String>> childForeignKeyFields = Optional.empty();

   private InlineParentSpec() {}

   public InlineParentSpec
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
