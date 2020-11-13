package org.sqljson.queries.result_types;

import java.util.ArrayList;
import java.util.List;


public class ResultTypeBuilder
{
   private final List<SimpleTableFieldProperty> simpleTableFieldProperties;
   private final List<TableExpressionProperty> tableExpressionProperties;
   private final List<ChildCollectionProperty> childCollectionProperties;
   private final List<ParentReferenceProperty> parentReferenceProperties;
   // NOTE: Fields from any inline parents are included in the appropriate
   //       collections above according to their source in the parent itself.

   public ResultTypeBuilder()
   {
      this.simpleTableFieldProperties = new ArrayList<>();
      this.tableExpressionProperties = new ArrayList<>();
      this.childCollectionProperties = new ArrayList<>();
      this.parentReferenceProperties = new ArrayList<>();
   }

   public void addSimpleTableFieldProperties(List<SimpleTableFieldProperty> tfs) { simpleTableFieldProperties.addAll(tfs); }

   public void addTableExpressionProperties(List<TableExpressionProperty> tofs) { tableExpressionProperties.addAll(tofs); }

   public void addChildCollectionProperties(List<ChildCollectionProperty> fs) { childCollectionProperties.addAll(fs); }

   public void addParentReferenceProperties(List<ParentReferenceProperty> fs) { parentReferenceProperties.addAll(fs); }

   public void addAllFieldsFrom
      (
         ResultType resultType,
         boolean forceNullable
      )
   {
      if ( !forceNullable )
      {
         addSimpleTableFieldProperties(resultType.getSimpleTableFieldProperties());
         addTableExpressionProperties(resultType.getTableExpressionProperties());
         addChildCollectionProperties(resultType.getChildCollectionProperties());
         addParentReferenceProperties(resultType.getParentReferenceProperties());
      }
      else
      {
         // Add nullable form of each field, as considered prior to any field
         // type overrides which are applied elsewhere (writing stage).
         // Expression fields are already nullable so need no transformation.
         addSimpleTableFieldProperties(resultType.getSimpleTableFieldPropertiesNullable());
         addTableExpressionProperties(resultType.getTableExpressionProperties());
         addChildCollectionProperties(resultType.getChildCollectionPropertiesNullable());
         addParentReferenceProperties(resultType.getParentReferencePropertiesNullable());
      }
   }

   public void addAllFieldsFrom(ResultTypeBuilder resultTypeBuilder)
   {
      addSimpleTableFieldProperties(resultTypeBuilder.simpleTableFieldProperties);
      addTableExpressionProperties(resultTypeBuilder.tableExpressionProperties);
      addChildCollectionProperties(resultTypeBuilder.childCollectionProperties);
      addParentReferenceProperties(resultTypeBuilder.parentReferenceProperties);
   }

   public ResultType build(String name)
   {
      return new ResultType(name, simpleTableFieldProperties, tableExpressionProperties, childCollectionProperties, parentReferenceProperties);
   }
}

