package org.sqljson.queries.specs;

import java.util.*;

import org.checkerframework.checker.nullness.qual.Nullable;

import com.fasterxml.jackson.annotation.JsonIgnore;

import static org.sqljson.common.util.Nullables.applyIfPresent;


public final class InlineParentSpec implements ParentSpec
{
   private final TableJsonSpec tableJson;
   private final @Nullable List<String> viaForeignKeyFields;
   private final @Nullable CustomJoinCondition customJoinCondition;

   private InlineParentSpec()
   {
      this.tableJson = new TableJsonSpec();
      this.viaForeignKeyFields = null;
      this.customJoinCondition = null;
   }

   public InlineParentSpec
      (
         TableJsonSpec tableJson,
         @Nullable List<String> viaForeignKeyFields
      )
   {
      this.tableJson = tableJson;
      this.viaForeignKeyFields = applyIfPresent(viaForeignKeyFields, Collections::unmodifiableList);
      this.customJoinCondition = null;
   }

   public InlineParentSpec
      (
         TableJsonSpec tableJson,
         @Nullable CustomJoinCondition customJoinCondition
      )
   {
      this.tableJson = tableJson;
      this.customJoinCondition = customJoinCondition;
      this.viaForeignKeyFields = null;
   }

   @Override
   public TableJsonSpec getTableJson() { return getParentTableJsonSpec(); }

   @Override
   public @Nullable List<String> getViaForeignKeyFields() { return viaForeignKeyFields; }

   @Override
   public @Nullable CustomJoinCondition getCustomJoinCondition() { return customJoinCondition; }

   @JsonIgnore
   public TableJsonSpec getParentTableJsonSpec() { return tableJson; }

   @JsonIgnore
   public @Nullable Set<String> getChildForeignKeyFieldsSet() { return applyIfPresent(viaForeignKeyFields, HashSet::new); }
}

