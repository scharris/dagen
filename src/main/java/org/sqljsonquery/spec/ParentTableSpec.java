package org.sqljsonquery.spec;

import java.util.Optional;
import java.util.Set;


public interface ParentTableSpec
{
   TableOutputSpec getParentTableOutputSpec();

   Optional<Set<String>> getChildForeignKeyFieldsSet();
}
