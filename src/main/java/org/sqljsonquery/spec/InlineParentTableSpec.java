package org.sqljsonquery.spec;

import java.util.*;

import com.fasterxml.jackson.annotation.JsonIgnore;


public final class InlineParentTableSpec
{
   private TableOutputSpec inlineParentTableOutputSpec;
   private Optional<List<String>> childForeignKeyFields = Optional.empty();
   private boolean omitChildRowWhenParentMissing = false;

   protected InlineParentTableSpec() {}

   public InlineParentTableSpec
   (
      TableOutputSpec inlineParentTableOutputSpec,
      Optional<List<String>> childForeignKeyFields,
      boolean omitChildRowWhenParentMissing
   )
   {
      this.inlineParentTableOutputSpec = inlineParentTableOutputSpec;
      this.childForeignKeyFields = childForeignKeyFields.map(Collections::unmodifiableList);
      this.omitChildRowWhenParentMissing = omitChildRowWhenParentMissing;
   }

   public TableOutputSpec getInlineParentTableOutputSpec() { return inlineParentTableOutputSpec; }

   public Optional<List<String>> getChildForeignKeyFields() { return childForeignKeyFields; }

   public boolean getOmitChildRowWhenParentMissing() { return omitChildRowWhenParentMissing; }

   @JsonIgnore
   public Optional<Set<String>> getChildForeignKeyFieldsSet() { return childForeignKeyFields.map(HashSet::new); }
}
