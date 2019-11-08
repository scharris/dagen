package io.sqljson.result_types;

import java.util.ArrayList;
import java.util.List;

import io.sqljson.dbmd.Field;
import io.sqljson.specs.queries.FieldTypeOverride;
import io.sqljson.specs.queries.TableOutputField;


public class GeneratedTypeBuilder
{
   private final List<DatabaseField> databaseFields;
   private final List<TableOutputField> expressionFields;
   private final List<ChildCollectionField> childCollectionFields;
   private final List<ParentReferenceField> parentReferenceFields;

   public GeneratedTypeBuilder()
   {
      this.databaseFields = new ArrayList<>();
      this.expressionFields = new ArrayList<>();
      this.childCollectionFields = new ArrayList<>();
      this.parentReferenceFields = new ArrayList<>();
   }

   public void addDatabaseField(String fieldName, Field f, List<FieldTypeOverride> typeOverrides)
   {
      databaseFields.add(new DatabaseField(fieldName, f, typeOverrides));
   }

   public void addDatabaseFields(List<DatabaseField> tfs) { databaseFields.addAll(tfs); }

   public void addExpressionField(TableOutputField tof)
   {
      expressionFields.add(tof);
   }

   public void addChildCollectionField(String fieldName, GeneratedType childType, boolean nullable)
   {
      childCollectionFields.add(new ChildCollectionField(fieldName, childType, nullable));
   }
   public void addChildCollectionFields(List<ChildCollectionField> fs) { childCollectionFields.addAll(fs); }

   public void addParentReferenceField(String fieldName, GeneratedType parentType, boolean nullable)
   {
      parentReferenceFields.add(new ParentReferenceField(fieldName, parentType, nullable));
   }
   public void addParentReferenceFields(List<ParentReferenceField> fs) { parentReferenceFields.addAll(fs); }

   public void addAllFieldsFrom
   (
      GeneratedType generatedType,
      boolean forceNullable
   )
   {
      if ( !forceNullable )
      {
         addDatabaseFields(generatedType.getDatabaseFields());
         addChildCollectionFields(generatedType.getChildCollectionFields());
         addParentReferenceFields(generatedType.getParentReferenceFields());
      }
      else
      {
         addDatabaseFields(generatedType.getDatabaseFieldsNullable());
         addChildCollectionFields(generatedType.getChildCollectionFieldsNullable());
         addParentReferenceFields(generatedType.getParentReferenceFieldsNullable());
      }
   }

   public GeneratedType build(String name)
   {
      return new GeneratedType(name, databaseFields, expressionFields, childCollectionFields, parentReferenceFields);
   }
}
