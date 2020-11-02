package org.sqljson.queries.specs;

import java.util.*;
import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;

import org.checkerframework.checker.nullness.qual.Nullable;

import com.fasterxml.jackson.annotation.JsonIgnore;

import org.sqljson.common.specs.RecordCondition;
import org.sqljson.common.specs.FieldParamCondition;


public final class TableJsonSpec
{
   private final String table; // possibly qualified

   private final @Nullable List<TableFieldExpr> fieldExpressions;

   private final @Nullable List<InlineParentSpec> inlineParentTables;

   private final @Nullable List<ReferencedParentSpec> referencedParentTables;

   private final @Nullable List<ChildCollectionSpec> childTableCollections;

   private final @Nullable List<FieldParamCondition> fieldParamConditions;

   private @Nullable RecordCondition recordCondition = null;

   TableJsonSpec()
   {
      this.table = "";
      this.fieldExpressions = null;
      this.inlineParentTables = null;
      this.referencedParentTables = null;
      this.childTableCollections = null;
      this.fieldParamConditions = null;
   }

   public TableJsonSpec
      (
         String table,
         @Nullable List<TableFieldExpr> fieldExpressions,
         @Nullable List<InlineParentSpec> inlineParentTables,
         @Nullable List<ReferencedParentSpec> referencedParentTables,
         @Nullable List<ChildCollectionSpec> childTableCollections,
         @Nullable List<FieldParamCondition> fieldParamConditions,
         @Nullable RecordCondition recordCondition
      )
   {
      requireNonNull(table);

      this.table = table;
      this.fieldExpressions = fieldExpressions != null ? unmodifiableList(fieldExpressions) : null;
      this.inlineParentTables = inlineParentTables != null ? unmodifiableList(new ArrayList<>(inlineParentTables)) : null;
      this.referencedParentTables = referencedParentTables != null ? unmodifiableList(new ArrayList<>(referencedParentTables)): null;
      this.childTableCollections = childTableCollections != null ? unmodifiableList(new ArrayList<>(childTableCollections)): null;
      this.fieldParamConditions = fieldParamConditions != null ? unmodifiableList(fieldParamConditions) : null;
      this.recordCondition = recordCondition;
   }

   /// The table name, possibly schema-qualified, of this output specification.
   public String getTable() { return table; }

   /// The output fields which originate from fields of this table.
   public @Nullable List<TableFieldExpr> getFieldExpressions() { return fieldExpressions; }

   @JsonIgnore
   public List<TableFieldExpr> getFieldExpressionsList()
   {
      return fieldExpressions != null ? fieldExpressions : emptyList();
   }

   public @Nullable List<InlineParentSpec> getInlineParentTables() { return inlineParentTables; }

   @JsonIgnore
   public List<InlineParentSpec> getInlineParentTablesList()
   {
      return inlineParentTables != null ? inlineParentTables : emptyList();
   }

   public @Nullable List<ReferencedParentSpec> getReferencedParentTables() { return referencedParentTables; }

   @JsonIgnore
   public List<ReferencedParentSpec> getReferencedParentTablesList()
   {
      return referencedParentTables != null ? referencedParentTables : emptyList();
   }

   public @Nullable List<ChildCollectionSpec> getChildTableCollections() { return childTableCollections; }

   @JsonIgnore
   public List<ChildCollectionSpec> getChildTableCollectionsList()
   {
      return childTableCollections != null ? childTableCollections : emptyList();
   }

   public @Nullable List<FieldParamCondition> getFieldParamConditions() { return fieldParamConditions; }

   @JsonIgnore
   public List<FieldParamCondition> getFieldParamConditionsList()
   {
      return fieldParamConditions != null ? fieldParamConditions : emptyList();
   }

   public @Nullable RecordCondition getRecordCondition() { return recordCondition; }

   @JsonIgnore
   public boolean hasCondition()
   {
      return
         fieldParamConditions != null && !fieldParamConditions.isEmpty() ||
         recordCondition != null;
   }

   @JsonIgnore
   public int getJsonPropertiesCount()
   {
       return
          (fieldExpressions != null ? fieldExpressions.size(): 0) +
          (childTableCollections != null ? childTableCollections.size(): 0) +
          (referencedParentTables != null ? referencedParentTables.size(): 0) +
          (inlineParentTables == null ? 0 :
           inlineParentTables.stream().mapToInt(ip -> ip.getParentTableJsonSpec().getJsonPropertiesCount()).sum());
   }
}

