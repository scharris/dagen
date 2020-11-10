package org.sqljson.queries.specs;

import java.util.List;
import java.util.Set;

import org.checkerframework.checker.nullness.qual.Nullable;

import com.fasterxml.jackson.annotation.JsonIgnore;


public interface ParentSpec
{
   TableJsonSpec getTableJson();

   @Nullable List<String> getViaForeignKeyFields();

   @Nullable CustomJoinCondition getCustomJoinCondition();

   @JsonIgnore
   TableJsonSpec getParentTableJsonSpec();

   @JsonIgnore
   @Nullable Set<String> getChildForeignKeyFieldsSet();
}
