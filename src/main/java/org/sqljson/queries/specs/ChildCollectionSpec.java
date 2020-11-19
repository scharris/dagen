package org.sqljson.queries.specs;

import java.util.*;

import org.checkerframework.checker.nullness.qual.Nullable;

import com.fasterxml.jackson.annotation.JsonIgnore;

import static org.sqljson.util.Nullables.applyIfPresent;


public final class ChildCollectionSpec
{
   private final String collectionName;
   private final TableJsonSpec tableJson;
   private final @Nullable List<String> foreignKeyFields;
   private final @Nullable CustomJoinCondition customJoinCondition;
   private final @Nullable String filter;
   private final @Nullable Boolean unwrap;
   private final @Nullable String orderBy;

   private ChildCollectionSpec()
   {
      this.collectionName = "";
      this.tableJson = new TableJsonSpec();
      this.foreignKeyFields = null;
      this.customJoinCondition = null;
      this.filter = null;
      this.unwrap = false;
      this.orderBy = null;
   }

   public ChildCollectionSpec
      (
         String collectionName,
         TableJsonSpec tableJson,
         @Nullable List<String> fkFields,
         @Nullable String filter,
         @Nullable Boolean unwrap,
         @Nullable String orderBy
      )
   {
      this.collectionName = collectionName;
      this.tableJson = tableJson;
      this.foreignKeyFields = applyIfPresent(fkFields, Collections::unmodifiableList);
      this.customJoinCondition = null;
      this.filter = filter;
      this.unwrap = unwrap;
      this.orderBy = orderBy;
   }

   public ChildCollectionSpec
      (
         String collectionName,
         TableJsonSpec tableJson,
         CustomJoinCondition customJoinCondition,
         @Nullable String filter,
         @Nullable Boolean unwrap,
         @Nullable String orderBy
      )
   {
      this.collectionName = collectionName;
      this.tableJson = tableJson;
      this.foreignKeyFields = null;
      this.customJoinCondition = customJoinCondition;
      this.filter = filter;
      this.unwrap = unwrap;
      this.orderBy = orderBy;
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

   public @Nullable Boolean getUnwrap() { return unwrap; }

   public @Nullable String getOrderBy() { return orderBy; }
}
