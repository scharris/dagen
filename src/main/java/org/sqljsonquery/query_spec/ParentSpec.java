package org.sqljsonquery.query_spec;

import java.util.Optional;
import java.util.Set;


public interface ParentSpec
{
   TableOutputSpec getParentTableOutputSpec();

   Optional<Set<String>> getChildForeignKeyFieldsSet();
}