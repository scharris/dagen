package org.sqljsonquery.queryspec;

import java.util.*;

import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;


public final class TableOutputSpec
{
   private String tableName; // possibly qualified

   private List<TableOutputField> nativeFields = emptyList();

   private List<InlineParentSpec> inlineParents = emptyList();

   private List<ReferencedParentSpec> referencedParents = emptyList();

   private List<ChildCollectionSpec> childCollections = emptyList();

   private Optional<String> filter = Optional.empty();

   protected TableOutputSpec() {}

   public TableOutputSpec
   (
      String tableName,
      List<TableOutputField> nativeFields,
      List<InlineParentSpec> inlineParents,
      List<ReferencedParentSpec> referencedParents,
      List<ChildCollectionSpec> childCollections,
      Optional<String> filter
   )
   {
      requireNonNull(tableName);
      requireNonNull(nativeFields);
      requireNonNull(inlineParents);
      requireNonNull(referencedParents);
      requireNonNull(childCollections);
      requireNonNull(filter);

      this.tableName = tableName;
      this.nativeFields = unmodifiableList(new ArrayList<>(nativeFields));
      this.inlineParents = unmodifiableList(new ArrayList<>(inlineParents));
      this.referencedParents = unmodifiableList(new ArrayList<>(referencedParents));
      this.childCollections = unmodifiableList(new ArrayList<>(childCollections));
      this.filter = filter;
   }

   /// The table name, possibly schema-qualified, of this output specification.
   public String getTableName() { return tableName; }

   /// The output fields which originate from fields of this table.
   public List<TableOutputField> getNativeFields() { return nativeFields; }

   public List<InlineParentSpec> getInlineParents() { return inlineParents; }

   public List<ReferencedParentSpec> getReferencedParents() { return referencedParents; }

   public List<ChildCollectionSpec> getChildCollections() { return childCollections; }

   public Optional<String> getFilter() { return filter; }
}
