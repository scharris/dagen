package org.sqljsonquery.types;

import java.util.*;
import static java.util.Collections.unmodifiableList;


public class GeneratedType
{
   private final String unqualifiedClassName;
   private final List<DatabaseField> databaseFields;
   private final List<ChildCollectionField> childCollectionFields;
   private final List<ParentReferenceField> parentReferenceFields;

   public GeneratedType
   (
      String unqualifiedClassName,
      List<DatabaseField> databaseFields,
      List<ChildCollectionField> childCollectionFields,
      List<ParentReferenceField> parentReferenceFields
   )
   {
      this.unqualifiedClassName = unqualifiedClassName;
      this.databaseFields = unmodifiableList(new ArrayList<>(databaseFields));
      this.childCollectionFields = unmodifiableList(new ArrayList<>(childCollectionFields));
      this.parentReferenceFields = unmodifiableList(new ArrayList<>(parentReferenceFields));
   }

   public String getTypeName() { return unqualifiedClassName; }

   public List<DatabaseField> getDatabaseFields() { return databaseFields; }

   public List<ChildCollectionField> getChildCollectionFields() { return childCollectionFields; }

   public List<ParentReferenceField> getParentReferenceFields() { return parentReferenceFields; }

   @Override
   public String toString()
   {
      return "GeneratedType{" +
         "unqualifiedClassName='" + unqualifiedClassName + '\'' +
         ", databaseFields=" + databaseFields +
         ", childCollectionFields=" + childCollectionFields +
         ", parentReferenceFields=" + parentReferenceFields +
         '}';
   }
}
