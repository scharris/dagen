package org.sqljson.queries.result_types;

import java.util.ArrayList;
import java.util.List;


public class ResultTypeBuilder
{
   private final List<DatabaseField> databaseFields;
   private final List<ExpressionField> expressionFields;
   private final List<ChildCollectionField> childCollectionFields;
   private final List<ParentReferenceField> parentReferenceFields;
   // NOTE: Fields from any inline parents are included in the appropriate
   //       collections above according to their source in the parent itself.

   public ResultTypeBuilder()
   {
      this.databaseFields = new ArrayList<>();
      this.expressionFields = new ArrayList<>();
      this.childCollectionFields = new ArrayList<>();
      this.parentReferenceFields = new ArrayList<>();
   }

   public void addDatabaseFields(List<DatabaseField> tfs) { databaseFields.addAll(tfs); }

   public void addExpressionFields(List<ExpressionField> tofs) { expressionFields.addAll(tofs); }

   public void addChildCollectionFields(List<ChildCollectionField> fs) { childCollectionFields.addAll(fs); }

   public void addParentReferenceFields(List<ParentReferenceField> fs) { parentReferenceFields.addAll(fs); }

   public void addAllFieldsFrom
      (
         ResultType resultType,
         boolean forceNullable
      )
   {
      if ( !forceNullable )
      {
         addDatabaseFields(resultType.getDatabaseFields());
         addExpressionFields(resultType.getExpressionFields());
         addChildCollectionFields(resultType.getChildCollectionFields());
         addParentReferenceFields(resultType.getParentReferenceFields());
      }
      else
      {
         // Add nullable form of each field, as considered prior to any field
         // type overrides which are applied elsewhere (writing stage).
         // Expression fields are already nullable so need no transformation.
         addDatabaseFields(resultType.getDatabaseFieldsNullable());
         addExpressionFields(resultType.getExpressionFields());
         addChildCollectionFields(resultType.getChildCollectionFieldsNullable());
         addParentReferenceFields(resultType.getParentReferenceFieldsNullable());
      }
   }

   public void addAllFieldsFrom(ResultTypeBuilder resultTypeBuilder)
   {
      addDatabaseFields(resultTypeBuilder.databaseFields);
      addExpressionFields(resultTypeBuilder.expressionFields);
      addChildCollectionFields(resultTypeBuilder.childCollectionFields);
      addParentReferenceFields(resultTypeBuilder.parentReferenceFields);
   }

   public ResultType build(String name)
   {
      return new ResultType(name, databaseFields, expressionFields, childCollectionFields, parentReferenceFields);
   }
}

