package org.sqljsonquery.spec;

import java.util.*;

import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;


public final class TableOutputSpec
{
   private String tableName; // possibly qualified

   private List<TableOutputField> fields = emptyList();

   private List<InlineParentTableSpec> inlineParents = emptyList();

   private List<WrappedParentTableSpec> wrappedParents = emptyList();

   private List<ChildTableSpec> childTables = emptyList();

   private Optional<String> filter = Optional.empty();

   protected TableOutputSpec() {}

   private TableOutputSpec
   (
      String tableName,
      List<TableOutputField> fields,
      List<InlineParentTableSpec> inlineParents,
      List<WrappedParentTableSpec> wrappedParents,
      List<ChildTableSpec> childTables,
      Optional<String> filter
   )
   {
      requireNonNull(tableName);
      requireNonNull(fields);
      requireNonNull(inlineParents);
      requireNonNull(wrappedParents);
      requireNonNull(childTables);
      requireNonNull(filter);

      this.tableName = tableName;
      this.fields = unmodifiableList(new ArrayList<>(fields));
      this.inlineParents = unmodifiableList(new ArrayList<>(inlineParents));
      this.wrappedParents = unmodifiableList(new ArrayList<>(wrappedParents));
      this.childTables = unmodifiableList(new ArrayList<>(childTables));
      this.filter = filter;
   }

   /// The table name, possibly schema-qualified, of this output specification.
   public String getTableName() { return tableName; }

   /// The output fields which originate from fields of this table.
   public List<TableOutputField> getFields() { return fields; }

   public List<InlineParentTableSpec> getInlineParents() { return inlineParents; }

   public List<WrappedParentTableSpec> getWrappedParents() { return wrappedParents; }

   public List<ChildTableSpec> getChildTables() { return childTables; }

   public Optional<String> getFilter() { return filter; }

}
