package org.sqljson.specs.queries;

import java.util.*;
import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;

import com.fasterxml.jackson.annotation.JsonIgnore;

import org.sqljson.specs.FieldParamCondition;


public final class TableOutputSpec
{
   private String tableName; // possibly qualified

   private List<TableOutputField> nativeFields = emptyList();

   private List<InlineParentSpec> inlineParents = emptyList();

   private List<ReferencedParentSpec> referencedParents = emptyList();

   private List<ChildCollectionSpec> childCollections = emptyList();

   private List<FieldParamCondition> fieldParamConditions = emptyList();

   private Optional<String> otherCondition = Optional.empty();

   private TableOutputSpec() {}

   public TableOutputSpec
   (
      String tableName,
      List<TableOutputField> nativeFields,
      List<InlineParentSpec> inlineParents,
      List<ReferencedParentSpec> referencedParents,
      List<ChildCollectionSpec> childCollections,
      List<FieldParamCondition> fieldParamConditions,
      Optional<String> otherCondition
   )
   {
      requireNonNull(tableName);
      requireNonNull(nativeFields);
      requireNonNull(inlineParents);
      requireNonNull(referencedParents);
      requireNonNull(childCollections);
      requireNonNull(fieldParamConditions);
      requireNonNull(otherCondition);

      this.tableName = tableName;
      this.nativeFields = unmodifiableList(new ArrayList<>(nativeFields));
      this.inlineParents = unmodifiableList(new ArrayList<>(inlineParents));
      this.referencedParents = unmodifiableList(new ArrayList<>(referencedParents));
      this.childCollections = unmodifiableList(new ArrayList<>(childCollections));
      this.fieldParamConditions = fieldParamConditions;
      this.otherCondition = otherCondition;
   }

   /// The table name, possibly schema-qualified, of this output specification.
   public String getTableName() { return tableName; }

   /// The output fields which originate from fields of this table.
   public List<TableOutputField> getNativeFields() { return nativeFields; }

   public List<InlineParentSpec> getInlineParents() { return inlineParents; }

   public List<ReferencedParentSpec> getReferencedParents() { return referencedParents; }

   public List<ChildCollectionSpec> getChildCollections() { return childCollections; }

   public List<FieldParamCondition> getFieldParamConditions() { return fieldParamConditions; }

   public Optional<String> getOtherCondition() { return otherCondition; }

   @JsonIgnore
   public boolean hasCondition() { return !fieldParamConditions.isEmpty() || otherCondition.isPresent(); }
}
