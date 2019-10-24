package gov.fda.nctr.sqljsonquery.spec;

import java.util.*;
import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;


public final class TableOutputSpec
{
   private String table;

   private List<TableOutputField> tableOutputFields = emptyList();

   private List<ChildTableSpec> childTableSpecs = emptyList();

   private List<ParentTableSpec> parentTableSpecs = emptyList();

   private Optional<String> filter = Optional.empty();

   protected TableOutputSpec() {}

   private TableOutputSpec
   (
      String table,
      List<TableOutputField> tableOutputFields,
      List<ChildTableSpec> childTableSpecs,
      List<ParentTableSpec> parentTableSpecs,
      Optional<String> filter
   )
   {
      requireNonNull(table);
      requireNonNull(tableOutputFields);
      requireNonNull(childTableSpecs);
      requireNonNull(parentTableSpecs);
      requireNonNull(filter);

      this.table = table;
      this.tableOutputFields = unmodifiableList(new ArrayList<>(tableOutputFields));
      this.childTableSpecs = unmodifiableList(new ArrayList<>(childTableSpecs));
      this.parentTableSpecs = unmodifiableList(new ArrayList<>(parentTableSpecs));
      this.filter = filter;
   }

   /// The table name, possibly schema-qualified, of this output specification.
   public String getTable() { return table; }

   /// The output fields which originate from fields of this table.
   public List<TableOutputField> getTableOutputFields() { return tableOutputFields; }

   public List<ChildTableSpec> getChildTableSpecs() { return childTableSpecs; }

   public List<ParentTableSpec> getParentTableSpecs() { return parentTableSpecs; }

   public Optional<String> getFilter() { return filter; }
}
