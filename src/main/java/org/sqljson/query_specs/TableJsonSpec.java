package org.sqljson.query_specs;

import java.util.*;
import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

import org.checkerframework.checker.nullness.qual.Nullable;

import com.fasterxml.jackson.annotation.JsonIgnore;


public final class TableJsonSpec
{
   private final String table; // possibly qualified

   private final @Nullable List<TableFieldExpr> fieldExpressions;

   private final @Nullable List<ParentSpec> parentTables;

   private final @Nullable List<ChildCollectionSpec> childTableCollections;

   private @Nullable RecordCondition recordCondition = null;

   TableJsonSpec()
   {
      this("", null, null, null, null);
   }

   public TableJsonSpec
      (
         String table,
         @Nullable List<TableFieldExpr> fieldExpressions,
         @Nullable List<ParentSpec> parentTables,
         @Nullable List<ChildCollectionSpec> childTableCollections,
         @Nullable RecordCondition recordCondition
      )
   {
      requireNonNull(table);

      this.table = table;
      this.fieldExpressions = fieldExpressions != null ? unmodifiableList(fieldExpressions) : null;
      this.parentTables = parentTables != null ? unmodifiableList(new ArrayList<>(parentTables)) : null;
      this.childTableCollections = childTableCollections != null ? unmodifiableList(new ArrayList<>(childTableCollections)): null;
      this.recordCondition = recordCondition;
   }

   /// The table name, possibly schema-qualified, of this output specification.
   public String getTable() { return table; }

   /// The output fields which originate from fields of this table.
   public @Nullable List<TableFieldExpr> getFieldExpressions() { return fieldExpressions; }

   public @Nullable List<ParentSpec> getParentTables() { return parentTables; }

   public @Nullable List<ChildCollectionSpec> getChildTableCollections()
   {
      return childTableCollections;
   }

   public @Nullable RecordCondition getRecordCondition() { return recordCondition; }


   @JsonIgnore
   public List<TableFieldExpr> getFieldExpressionsList()
   {
      return fieldExpressions != null ? fieldExpressions : emptyList();
   }

   @JsonIgnore
   public List<ParentSpec> getParentTablesList()
   {
      return parentTables != null ? parentTables : emptyList();
   }

   @JsonIgnore
   public List<ParentSpec> getReferencedParentTablesList()
   {
      return parentTables != null ?
         parentTables.stream().filter(t -> t.getReferenceName() != null).collect(toList())
         : emptyList();
   }

   @JsonIgnore
   public List<ParentSpec> getInlineParentTablesList()
   {
      return parentTables != null ?
         parentTables.stream().filter(t -> t.getReferenceName() == null).collect(toList())
         : emptyList();
   }

   @JsonIgnore
   public List<ChildCollectionSpec> getChildTableCollectionsList()
   {
      return childTableCollections != null ? childTableCollections : emptyList();
   }

   @JsonIgnore
   public boolean hasCondition()
   {
      return recordCondition != null;
   }

   @JsonIgnore
   public int getJsonPropertiesCount()
   {
       return
          (fieldExpressions != null ? fieldExpressions.size(): 0) +
          (childTableCollections != null ? childTableCollections.size(): 0) +
          getReferencedParentTablesList().size() +
          getInlineParentTablesList().stream()
             .mapToInt(ip -> ip.getParentTableJsonSpec().getJsonPropertiesCount())
             .sum();
   }
}

