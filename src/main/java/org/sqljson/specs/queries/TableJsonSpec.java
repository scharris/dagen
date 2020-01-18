package org.sqljson.specs.queries;

import java.util.*;
import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;

import com.fasterxml.jackson.annotation.JsonIgnore;

import org.sqljson.specs.FieldParamCondition;


public final class TableJsonSpec
{
   private String table; // possibly qualified

   private List<TableFieldExpr> fieldExpressions = emptyList();

   private List<InlineParentSpec> inlineParentTables = emptyList();

   private List<ReferencedParentSpec> referencedParentTables = emptyList();

   private List<ChildCollectionSpec> childTableCollections = emptyList();

   private List<FieldParamCondition> fieldParamConditions = emptyList();

   private Optional<String> condition = Optional.empty();

   private TableJsonSpec() {}

   public TableJsonSpec
   (
      String table,
      List<TableFieldExpr> fieldExpressions,
      List<InlineParentSpec> inlineParentTables,
      List<ReferencedParentSpec> referencedParentTables,
      List<ChildCollectionSpec> childTableCollections,
      List<FieldParamCondition> fieldParamConditions,
      Optional<String> condition
   )
   {
      requireNonNull(table);
      requireNonNull(fieldExpressions);
      requireNonNull(inlineParentTables);
      requireNonNull(referencedParentTables);
      requireNonNull(childTableCollections);
      requireNonNull(fieldParamConditions);
      requireNonNull(condition);

      this.table = table;
      this.fieldExpressions = unmodifiableList(new ArrayList<>(fieldExpressions));
      this.inlineParentTables = unmodifiableList(new ArrayList<>(inlineParentTables));
      this.referencedParentTables = unmodifiableList(new ArrayList<>(referencedParentTables));
      this.childTableCollections = unmodifiableList(new ArrayList<>(childTableCollections));
      this.fieldParamConditions = fieldParamConditions;
      this.condition = condition;
   }

   /// The table name, possibly schema-qualified, of this output specification.
   public String getTable() { return table; }

   /// The output fields which originate from fields of this table.
   public List<TableFieldExpr> getFieldExpressions() { return fieldExpressions; }

   public List<InlineParentSpec> getInlineParentTables() { return inlineParentTables; }

   public List<ReferencedParentSpec> getReferencedParentTables() { return referencedParentTables; }

   public List<ChildCollectionSpec> getChildTableCollections() { return childTableCollections; }

   public List<FieldParamCondition> getFieldParamConditions() { return fieldParamConditions; }

   public Optional<String> getCondition() { return condition; }

   @JsonIgnore
   public boolean hasCondition() { return !fieldParamConditions.isEmpty() || condition.isPresent(); }

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
