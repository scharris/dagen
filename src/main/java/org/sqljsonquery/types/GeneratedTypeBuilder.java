package org.sqljsonquery.types;

import java.util.ArrayList;
import java.util.List;

import org.sqljsonquery.dbmd.Field;


public class GeneratedTypeBuilder
{
   private final String unqualifiedClassName;
   private final List<DatabaseField> databaseFields;
   private final List<ChildCollectionField> childCollectionFields;
   private final List<ParentReferenceField> parentReferenceFields;

   public GeneratedTypeBuilder(String uqClassName)
   {
      this.unqualifiedClassName = uqClassName;
      this.databaseFields = new ArrayList<>();
      this.childCollectionFields = new ArrayList<>();
      this.parentReferenceFields = new ArrayList<>();
   }

   public void addDatabaseField(String fieldName, Field f) { databaseFields.add(new DatabaseField(fieldName, f)); }
   public void addDatabaseFields(List<DatabaseField> tfs) { databaseFields.addAll(tfs); }

   public void addChildCollectionField(String fieldName, GeneratedType childType)
   {
      childCollectionFields.add(new ChildCollectionField(fieldName, childType));
   }
   public void addChildCollectionFields(List<ChildCollectionField> fs) { childCollectionFields.addAll(fs); }

   public void addParentReferenceField(String fieldName, GeneratedType parentType, boolean nullable)
   {
      parentReferenceFields.add(new ParentReferenceField(fieldName, parentType, nullable));
   }
   public void addParentReferenceFields(List<ParentReferenceField> fs) { parentReferenceFields.addAll(fs); }

   public GeneratedType build()
   {
      return new GeneratedType(unqualifiedClassName, databaseFields, childCollectionFields, parentReferenceFields);
   }
}
