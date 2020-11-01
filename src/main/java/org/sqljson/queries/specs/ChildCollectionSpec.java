package org.sqljson.queries.specs;

import java.util.*;

import org.checkerframework.checker.nullness.qual.Nullable;
import com.fasterxml.jackson.annotation.JsonIgnore;

import static org.sqljson.common.util.Nullables.applyIfPresent;


public final class ChildCollectionSpec
{
   private String collectionName;
   private TableJsonSpec tableJson;
   private @Nullable List<String> foreignKeyFields = null;
   private @Nullable CustomJoinCondition customJoinCondition = null;
   private @Nullable String filter = null;
   private boolean unwrap = false;

   private ChildCollectionSpec()
   {
      this.collectionName = "";
      this.tableJson = new TableJsonSpec();
   }

   public ChildCollectionSpec
      (
         String collectionName,
         TableJsonSpec tableJson,
         @Nullable List<String> fkFields,
         @Nullable String filter,
         boolean unwrap
      )
   {
      this.collectionName = collectionName;
      this.tableJson = tableJson;
      this.foreignKeyFields = applyIfPresent(fkFields, Collections::unmodifiableList);
      this.filter = filter;
      this.unwrap = unwrap;
   }

   public ChildCollectionSpec
      (
         String collectionName,
         TableJsonSpec tableJson,
         CustomJoinCondition customJoinCondition,
         @Nullable String filter,
         boolean unwrap
      )
   {
      this.collectionName = collectionName;
      this.tableJson = tableJson;
      this.foreignKeyFields = null;
      this.customJoinCondition = customJoinCondition;
      this.filter = filter;
      this.unwrap = unwrap;
   }

   public String getCollectionName() { return collectionName; }

   public TableJsonSpec getTableJson() { return tableJson; }

   public @Nullable List<String> getForeignKeyFields() { return foreignKeyFields; }

   public @Nullable CustomJoinCondition getCustomJoinCondition() { return customJoinCondition; }

   @JsonIgnore
   public @Nullable Set<String> getForeignKeyFieldsSet()
   {
      return applyIfPresent(foreignKeyFields, HashSet::new);
   }

   public @Nullable String getFilter() { return filter; }

   public boolean getUnwrap() { return unwrap; }
}

