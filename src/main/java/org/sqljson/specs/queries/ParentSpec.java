package org.sqljson.specs.queries;

import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;


public interface ParentSpec
{
   TableJsonSpec getParentTableJsonSpec();

   @Nullable Set<String> getChildForeignKeyFieldsSet();
}
