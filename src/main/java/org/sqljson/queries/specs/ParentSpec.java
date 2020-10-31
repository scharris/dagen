package org.sqljson.queries.specs;

import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;


public interface ParentSpec
{
   TableJsonSpec getParentTableJsonSpec();

   @Nullable Set<String> getChildForeignKeyFieldsSet();
}
