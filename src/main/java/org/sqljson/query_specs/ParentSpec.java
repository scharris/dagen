package org.sqljson.query_specs;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.checkerframework.checker.nullness.qual.Nullable;

import com.fasterxml.jackson.annotation.JsonIgnore;

import static org.sqljson.util.Nullables.applyIfPresent;


public final class ParentSpec
{
   private final TableJsonSpec tableJson;
   private final @Nullable String referenceName;
   private final @Nullable List<String> viaForeignKeyFields;
   private final @Nullable CustomJoinCondition customJoinCondition;

   private ParentSpec()
   {
      this(new TableJsonSpec(), null, null, null);
   }

   public ParentSpec
      (
         TableJsonSpec tableJson,
         @Nullable String referenceName,
         @Nullable List<String> viaForeignKeyFields
      )
   {
      this(tableJson, referenceName, viaForeignKeyFields, null);
   }

   public ParentSpec
      (
         TableJsonSpec tableJson,
         @Nullable String referenceName,
         @Nullable List<String> viaForeignKeyFields,
         @Nullable CustomJoinCondition customJoinCondition
      )
   {
      this.tableJson = tableJson;
      this.referenceName = referenceName;
      this.viaForeignKeyFields = viaForeignKeyFields;
      this.customJoinCondition = customJoinCondition;
   }
   public TableJsonSpec getTableJson() { return getParentTableJsonSpec(); }

   public @Nullable String getReferenceName() { return referenceName; }

   public @Nullable List<String> getViaForeignKeyFields() { return viaForeignKeyFields; }

   public @Nullable CustomJoinCondition getCustomJoinCondition() { return customJoinCondition; }

   @JsonIgnore
   public TableJsonSpec getParentTableJsonSpec() { return tableJson; }

   @JsonIgnore
   public @Nullable Set<String> getChildForeignKeyFieldsSet() { return applyIfPresent(viaForeignKeyFields, HashSet::new); }
}

