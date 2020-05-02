package org.sqljson.specs.queries;

import java.util.*;

import org.checkerframework.checker.nullness.qual.Nullable;
import com.fasterxml.jackson.annotation.JsonIgnore;

import static org.sqljson.util.Nullables.applyIfPresent;


public final class InlineParentSpec implements ParentSpec
{
   private TableJsonSpec tableJson;
   private @Nullable List<String> viaForeignKeyFields = null;
   private @Nullable CustomJoinCondition customJoinCondition = null;

   private InlineParentSpec()
   {
      tableJson = new TableJsonSpec();
   }

   public InlineParentSpec
   (
      TableJsonSpec tableJson,
      @Nullable List<String> viaForeignKeyFields
   )
   {
      this.tableJson = tableJson;
      this.viaForeignKeyFields = applyIfPresent(viaForeignKeyFields, Collections::unmodifiableList);
   }

   public InlineParentSpec
   (
      TableJsonSpec tableJson,
      @Nullable CustomJoinCondition customJoinCondition
   )
   {
      this.tableJson = tableJson;
      this.customJoinCondition = customJoinCondition;
   }

   public TableJsonSpec getTableJson() { return getParentTableJsonSpec(); }

   @JsonIgnore
   public TableJsonSpec getParentTableJsonSpec() { return tableJson; }

   @JsonIgnore
   public @Nullable Set<String> getChildForeignKeyFieldsSet() { return applyIfPresent(viaForeignKeyFields, HashSet::new); }

   public @Nullable CustomJoinCondition getCustomJoinCondition() { return customJoinCondition; }
}
