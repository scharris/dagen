package org.sqljsonquery.queryspec;

import java.util.Optional;
import java.util.Set;


public interface ParentSpec
{
   TableOutputSpec getParentTableOutputSpec();

   Optional<Set<String>> getChildForeignKeyFieldsSet();
}