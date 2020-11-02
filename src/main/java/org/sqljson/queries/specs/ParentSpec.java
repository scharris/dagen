package org.sqljson.queries.specs;

import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;
import com.fasterxml.jackson.annotation.JsonIgnore;


public interface ParentSpec
{
   @JsonIgnore
   TableJsonSpec getParentTableJsonSpec();

   @JsonIgnore
   @Nullable Set<String> getChildForeignKeyFieldsSet();
}
