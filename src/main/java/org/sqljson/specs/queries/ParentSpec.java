package org.sqljson.specs.queries;

import java.util.Optional;
import java.util.Set;


public interface ParentSpec
{
   TableJsonSpec getParentTableJsonSpec();

   Optional<Set<String>> getChildForeignKeyFieldsSet();
}
