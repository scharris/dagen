package org.sqljson.specs.queries;

import java.util.*;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.fasterxml.jackson.annotation.JsonIgnore;

import static org.sqljson.util.Nullables.applyIfPresent;


public final class ReferencedParentSpec implements ParentSpec
{
   private String referenceName;
   private TableJsonSpec tableJson;
   private @Nullable List<String> viaForeignKeyFields = null;
   private @Nullable CustomJoinCondition customJoinCondition = null;

   private ReferencedParentSpec()
   {
      this.referenceName = "";
      this.tableJson = new TableJsonSpec();
   }

   public ReferencedParentSpec
   (
      String referenceName,
      TableJsonSpec tableJson,
      @Nullable List<String> viaForeignKeyFields
   )
   {
      this.referenceName = referenceName;
      this.tableJson = tableJson;
      this.viaForeignKeyFields = applyIfPresent(viaForeignKeyFields, Collections::unmodifiableList);
   }

   public ReferencedParentSpec
       (
           String referenceName,
           TableJsonSpec tableJson,
           @Nullable CustomJoinCondition customJoinCondition
       )
   {
      this.referenceName = referenceName;
      this.tableJson = tableJson;
      this.customJoinCondition = customJoinCondition;
   }

   public String getReferenceName() { return referenceName; }

   public TableJsonSpec getTableJson() { return getParentTableJsonSpec(); }

   public @Nullable List<String> getViaForeignKeyFields() { return viaForeignKeyFields; }

   @JsonIgnore
   public TableJsonSpec getParentTableJsonSpec() { return tableJson; }

   @JsonIgnore
   public @Nullable Set<String> getChildForeignKeyFieldsSet() { return applyIfPresent(viaForeignKeyFields, HashSet::new); }

   public @Nullable CustomJoinCondition getCustomJoinCondition() { return customJoinCondition; }
}
