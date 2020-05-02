package org.sqljson.specs.queries;

import java.util.*;
import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;

import org.checkerframework.checker.nullness.qual.Nullable;

import com.fasterxml.jackson.annotation.JsonIgnore;

import org.sqljson.specs.RecordCondition;
import org.sqljson.specs.FieldParamCondition;


public final class TableJsonSpec
{
   private String table; // possibly qualified

   private List<TableFieldExpr> fieldExpressions = emptyList();

   private List<InlineParentSpec> inlineParentTables = emptyList();

   private List<ReferencedParentSpec> referencedParentTables = emptyList();

   private List<ChildCollectionSpec> childTableCollections = emptyList();

   private List<FieldParamCondition> fieldParamConditions = emptyList();

   private @Nullable RecordCondition recordCondition = null;

   TableJsonSpec()
   {
      this.table = "";
   }

   public TableJsonSpec
      (
         String table,
         List<TableFieldExpr> fieldExpressions,
         List<InlineParentSpec> inlineParentTables,
         List<ReferencedParentSpec> referencedParentTables,
         List<ChildCollectionSpec> childTableCollections,
         List<FieldParamCondition> fieldParamConditions,
         @Nullable RecordCondition recordCondition
      )
   {
      requireNonNull(table);
      requireNonNull(fieldExpressions);
      requireNonNull(inlineParentTables);
      requireNonNull(referencedParentTables);
      requireNonNull(childTableCollections);
      requireNonNull(fieldParamConditions);

      this.table = table;
      this.fieldExpressions = unmodifiableList(fieldExpressions);
      this.inlineParentTables = unmodifiableList(new ArrayList<>(inlineParentTables));
      this.referencedParentTables = unmodifiableList(new ArrayList<>(referencedParentTables));
      this.childTableCollections = unmodifiableList(new ArrayList<>(childTableCollections));
      this.fieldParamConditions = fieldParamConditions;
      this.recordCondition = recordCondition;
   }

   /// The table name, possibly schema-qualified, of this output specification.
   public String getTable() { return table; }

   /// The output fields which originate from fields of this table.
   public List<TableFieldExpr> getFieldExpressions() { return fieldExpressions; }

   public List<InlineParentSpec> getInlineParentTables() { return inlineParentTables; }

   public List<ReferencedParentSpec> getReferencedParentTables() { return referencedParentTables; }

   public List<ChildCollectionSpec> getChildTableCollections() { return childTableCollections; }

   public List<FieldParamCondition> getFieldParamConditions() { return fieldParamConditions; }

   public @Nullable RecordCondition getRecordCondition() { return recordCondition; }

   @JsonIgnore
   public boolean hasCondition() { return !fieldParamConditions.isEmpty() || recordCondition != null; }

   @JsonIgnore
   public int getJsonPropertiesCount()
   {
       return
          fieldExpressions.size() +
          childTableCollections.size() +
          referencedParentTables.size() +
          inlineParentTables.stream().mapToInt(ip -> ip.getParentTableJsonSpec().getJsonPropertiesCount()).sum();
   }
}

