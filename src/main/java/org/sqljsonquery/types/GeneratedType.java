package org.sqljsonquery.types;

import java.util.*;

import static java.util.Collections.unmodifiableList;
import static java.util.stream.Collectors.toList;


public class GeneratedType
{
   private final String typeName; // always unqualified by module/package
   private final List<DatabaseField> databaseFields;
   private final List<ChildCollectionField> childCollectionFields;
   private final List<ParentReferenceField> parentReferenceFields;

   public GeneratedType
   (
      String typeName,
      List<DatabaseField> databaseFields,
      List<ChildCollectionField> childCollectionFields,
      List<ParentReferenceField> parentReferenceFields
   )
   {
      this.typeName = typeName;
      this.databaseFields = unmodifiableList(new ArrayList<>(databaseFields));
      this.childCollectionFields = unmodifiableList(new ArrayList<>(childCollectionFields));
      this.parentReferenceFields = unmodifiableList(new ArrayList<>(parentReferenceFields));
   }

   public String getTypeName() { return typeName; }

   public List<DatabaseField> getDatabaseFields() { return databaseFields; }

   /// Get nullable variants of the database fields.
   public List<DatabaseField> getDatabaseFieldsNullable()
   {
      return databaseFields.stream().map(DatabaseField::toNullable).collect(toList());
   }

   public List<ChildCollectionField> getChildCollectionFields() { return childCollectionFields; }

   /// Get nullable variants of the child collection fields.
   public List<ChildCollectionField> getChildCollectionFieldsNullable()
   {
      return childCollectionFields.stream().map(ChildCollectionField::toNullable).collect(toList());
   }


   public List<ParentReferenceField> getParentReferenceFields() { return parentReferenceFields; }

   /// Get nullable variants of the parent reference fields.
   public List<ParentReferenceField> getParentReferenceFieldsNullable()
   {
      return parentReferenceFields.stream().map(ParentReferenceField::toNullable).collect(toList());
   }

   @Override
   public String toString()
   {
      return "GeneratedType{" +
         "typeName='" + typeName + '\'' +
         ", databaseFields=" + databaseFields +
         ", childCollectionFields=" + childCollectionFields +
         ", parentReferenceFields=" + parentReferenceFields +
         '}';
   }
}
