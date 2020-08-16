package org.sqljson.result_types;

import java.util.ArrayList;
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;

import org.sqljson.dbmd.Field;

public class GeneratedTypeBuilder
{
   private final List<DatabaseField> databaseFields;
   private final List<ExpressionField> expressionFields;
   private final List<ChildCollectionField> childCollectionFields;
   private final List<ParentReferenceField> parentReferenceFields;
   // NOTE: Fields from inline parents are included in the above.

   public GeneratedTypeBuilder()
   {
      this.databaseFields = new ArrayList<>();
      this.expressionFields = new ArrayList<>();
      this.childCollectionFields = new ArrayList<>();
      this.parentReferenceFields = new ArrayList<>();
   }

   public void addDatabaseField
      (
         String fieldName,
         Field f,
         @Nullable String generatedFieldType
      )
   {
      databaseFields.add(new DatabaseField(fieldName, f, generatedFieldType));
   }

   public void addDatabaseFields(List<DatabaseField> tfs) { databaseFields.addAll(tfs); }

   public void addExpressionField(ExpressionField tof)
   {
      expressionFields.add(tof);
   }
   public void addExpressionField
      (
         String name,
         @Nullable String fieldExpr,
         @Nullable String generatedFieldType
      )
   {
      addExpressionField(new ExpressionField(name, fieldExpr, generatedFieldType));
   }

   public void addExpressionFields(List<ExpressionField> tofs) { expressionFields.addAll(tofs); }

   public void addChildCollectionField
      (
         String fieldName,
         GeneratedType childType,
         boolean nullable
      )
   {
      childCollectionFields.add(new ChildCollectionField(fieldName, childType, nullable));
   }
   public void addChildCollectionFields(List<ChildCollectionField> fs) { childCollectionFields.addAll(fs); }

   public void addParentReferenceField
      (
         String fieldName,
         GeneratedType parentType,
         boolean nullable
      )
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
         addExpressionFields(generatedType.getExpressionFields());
         addChildCollectionFields(generatedType.getChildCollectionFields());
         addParentReferenceFields(generatedType.getParentReferenceFields());
      }
      else
      {
         // Add nullable form of each field, as considered prior to any field
         // type overrides which are applied elsewhere (writing stage).
         // Expression fields are already nullable so need no transformation.
         addDatabaseFields(generatedType.getDatabaseFieldsNullable());
         addExpressionFields(generatedType.getExpressionFields());
         addChildCollectionFields(generatedType.getChildCollectionFieldsNullable());
         addParentReferenceFields(generatedType.getParentReferenceFieldsNullable());
      }
   }

   public void addAllFieldsFrom(GeneratedTypeBuilder generatedTypeBuilder)
   {
      addDatabaseFields(generatedTypeBuilder.databaseFields);
      addExpressionFields(generatedTypeBuilder.expressionFields);
      addChildCollectionFields(generatedTypeBuilder.childCollectionFields);
      addParentReferenceFields(generatedTypeBuilder.parentReferenceFields);
   }

   public GeneratedType build(String name)
   {
      return new GeneratedType(name, databaseFields, expressionFields, childCollectionFields, parentReferenceFields);
   }
}

