package org.sqljson.queries.result_types;

import java.util.*;
import java.util.function.Function;
import static java.util.Collections.emptyMap;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.*;
import static java.util.function.Function.identity;

import org.checkerframework.checker.nullness.qual.Nullable;

import static org.sqljson.common.util.Nullables.*;
import org.sqljson.common.util.StringFuns;
import org.sqljson.dbmd.*;
import org.sqljson.queries.specs.*;


public class ResultTypesGenerator
{
   private final DatabaseMetadata dbmd;
   private final @Nullable String defaultSchema;
   private final Function<String,String> defaultPropertyNameFn;

   public ResultTypesGenerator
      (
         DatabaseMetadata dbmd,
         @Nullable String defaultSchema,
         Function<String,String> defaultPropertyNameFn
      )
   {
      this.dbmd = dbmd;
      this.defaultSchema = defaultSchema;
      this.defaultPropertyNameFn = defaultPropertyNameFn;
   }

   public List<ResultType> generateResultTypes(TableJsonSpec tjs)
   {
      return this.generateResultTypes(tjs, emptyMap());
   }

   @SuppressWarnings("keyfor")
   private List<ResultType> generateResultTypes
      (
         TableJsonSpec tjs,
         Map<String,ResultType> previouslyGeneratedTypesByName
      )
   {
      Map<String,ResultType> typesInScope = new HashMap<>(previouslyGeneratedTypesByName);

      ResultTypeBuilder typeBuilder = new ResultTypeBuilder();
      List<ResultType> resultTypes = new ArrayList<>();

      RelId relId = dbmd.toRelId(tjs.getTable(), defaultSchema);

      // Add the table's own fields and expressions involving those fields.
      typeBuilder.addSimpleTableFieldProperties(getSimpleTableFieldProperties(relId, tjs.getFieldExpressionsList()));
      typeBuilder.addTableExpressionProperties(getTableExpressionProperties(relId, tjs.getFieldExpressionsList()));

      // Inline parents can contribute fields to any primary field category (table field,
      // expression, parent ref, child collection). Get the inline parent fields, and the result
      // types from the tables themselves and recursively from their specified related tables.
      InlineParentContrs inlineParentsContr = getInlineParentContrs(relId, tjs.getInlineParentTablesList(), typesInScope);
      typeBuilder.addAllFieldsFrom(inlineParentsContr.typeBuilder);
      resultTypes.addAll(inlineParentsContr.resultTypes);
      inlineParentsContr.resultTypes.forEach(t -> typesInScope.put(t.getTypeName(), t));

      // Get referenced parent fields and result types, with result types from related tables.
      RefdParentContrs refdParentsContr = getRefdParentContrs(relId, tjs.getReferencedParentTablesList(), typesInScope);
      typeBuilder.addParentReferenceProperties(refdParentsContr.parentReferenceProperties);
      resultTypes.addAll(refdParentsContr.resultTypes);
      inlineParentsContr.resultTypes.forEach(t -> typesInScope.put(t.getTypeName(), t));

      // Get the child collection fields and result types, with result types from related tables.
      ChildCollectionContrs childCollsContr = getChildCollectionContrs(tjs.getChildTableCollectionsList(), typesInScope);
      typeBuilder.addChildCollectionProperties(childCollsContr.childCollectionProperties);
      resultTypes.addAll(childCollsContr.resultTypes);
      inlineParentsContr.resultTypes.forEach(t -> typesInScope.put(t.getTypeName(), t));

      // The top table's type must be added at leading position in the returned list.
      // If the type is essentially identical to one already in scope, ignoring only any name
      // extension added to make the name unique, then add the previously generated instance
      // instead.
      String baseTypeName = StringFuns.upperCamelCase(tjs.getTable()); // Base type name is the desired name, without any trailing digits.
      if ( !typesInScope.containsKey(baseTypeName) ) // No previously generated type of same base name.
         resultTypes.add(0, typeBuilder.build(baseTypeName));
      else
      {
         @Nullable ResultType existingIdenticalType =
            findTypeIgnoringNameExtensions(typeBuilder.build(baseTypeName), previouslyGeneratedTypesByName);
         if ( existingIdenticalType != null ) // Identical previously generated type found, use it as top type.
            resultTypes.add(0, existingIdenticalType);
         else // This type does not match any previously generated, but needs a new name.
         {
            String uniqueName = StringFuns.makeNameNotInSet(baseTypeName, typesInScope.keySet(), "_");
            resultTypes.add(0, typeBuilder.build(uniqueName));
         }
      }

      return resultTypes;
   }

   private List<SimpleTableFieldProperty> getSimpleTableFieldProperties
      (
         RelId relId,
         List<TableFieldExpr> tableFieldExpressions
      )
   {
      List<SimpleTableFieldProperty> fields = new ArrayList<>();

      Map<String,Field> dbFieldsByName = getTableFieldsByName(relId);

      for ( TableFieldExpr tfe : tableFieldExpressions )
      {
         if ( tfe.getField() != null )
         {
            Field dbField = requireNonNull(dbFieldsByName.get(dbmd.normalizeName(requireNonNull(tfe.getField()))),
               "no metadata for field " + relId + "." + tfe.getField());
            fields.add(new SimpleTableFieldProperty(getOutputFieldName(tfe, dbField), dbField, tfe.getGeneratedFieldType()));
         }
      }

      return fields;
   }

   private List<TableExpressionProperty> getTableExpressionProperties
      (
         RelId relId,
         List<TableFieldExpr> tableFieldExpressions
      )
   {
      List<TableExpressionProperty> fields = new ArrayList<>();

      for ( TableFieldExpr tfe : tableFieldExpressions )
      {
         if ( tfe.getExpression() != null )
         {
            String jsonProperty = valueOrThrow(tfe.getJsonProperty(), () ->
                new RuntimeException("Expression field " + relId + "." + tfe + " requires a json property.")
            );
            fields.add(new TableExpressionProperty(jsonProperty, tfe.getExpression(), tfe.getGeneratedFieldType()));
         }
      }

      return fields;
   }

   // Build the inline parents part of the generated type.
   private InlineParentContrs getInlineParentContrs
      (
         RelId relId,
         List<InlineParentSpec> inlineParentSpecs,
         Map<String,ResultType> envTypesInScope
      )
   {
      ResultTypeBuilder typeBuilder = new ResultTypeBuilder();
      List<ResultType> resultTypes = new ArrayList<>();

      Map<String,ResultType> typesInScope = new HashMap<>(envTypesInScope);

      for ( InlineParentSpec parentSpec :  inlineParentSpecs )
      {
         // Generate types for the parent table and any related tables it includes recursively.
         List<ResultType> parentResultTypes = generateResultTypes(parentSpec.getParentTableJsonSpec(), typesInScope);
         ResultType parentType = parentResultTypes.get(0); // will not be generated

         // If the parent record might be absent, then all inline fields must be nullable.
         boolean forceNullable =
            parentSpec.getParentTableJsonSpec().hasCondition() ||
            noFkFieldKnownNotNullable(relId, parentSpec);

         typeBuilder.addAllFieldsFrom(parentType, forceNullable);

         List<ResultType> actuallyGeneratedParentTypes = parentResultTypes.subList(1, parentResultTypes.size());
         resultTypes.addAll(actuallyGeneratedParentTypes);
         actuallyGeneratedParentTypes.forEach(t -> typesInScope.put(t.getTypeName(), t));
      }

      return new InlineParentContrs(typeBuilder, resultTypes);
   }

   /// Get fields and types from the given referenced parents.
   private RefdParentContrs getRefdParentContrs
      (
         RelId relId,
         List<ReferencedParentSpec> referencedParentSpecs,
         Map<String, ResultType> envTypesInScope
      )
   {
      List<ParentReferenceProperty> parentRefFields = new ArrayList<>();
      List<ResultType> resultTypes = new ArrayList<>();

      Map<String, ResultType> typesInScope = new HashMap<>(envTypesInScope);

      for ( ReferencedParentSpec parentSpec : referencedParentSpecs )
      {
         // Generate types by traversing the parent table and its parents and children.
         List<ResultType> parentResultTypes = generateResultTypes(parentSpec.getParentTableJsonSpec(), typesInScope);
         ResultType parentType = parentResultTypes.get(0);

         boolean forceNullable =
            parentSpec.getParentTableJsonSpec().hasCondition() ||
            noFkFieldKnownNotNullable(relId, parentSpec);

         parentRefFields.add(new ParentReferenceProperty(parentSpec.getReferenceName(), parentType, forceNullable));

         resultTypes.addAll(parentResultTypes);
         parentResultTypes.forEach(t -> typesInScope.put(t.getTypeName(), t));
      }

      return new RefdParentContrs(parentRefFields, resultTypes);
   }

   private ChildCollectionContrs getChildCollectionContrs
      (
         List<ChildCollectionSpec> childCollectionSpecs,
         Map<String, ResultType> envTypesInScope
      )
   {
      List<ChildCollectionProperty> childCollectionProperties = new ArrayList<>();
      List<ResultType> resultTypes = new ArrayList<>();

      Map<String, ResultType> typesInScope = new HashMap<>(envTypesInScope);

      for ( ChildCollectionSpec childCollSpec : childCollectionSpecs )
      {
         // Generate types by traversing the child table and its parents and children recursively.
         List<ResultType> childResultTypes = generateResultTypes(childCollSpec.getTableJson(), typesInScope);

         // Mark the top-level child type as unwrapped if specified.
         ResultType childType = childResultTypes.get(0).withUnwrapped(valueOr(childCollSpec.getUnwrap(), false));
         childResultTypes.set(0, childType);

         childCollectionProperties.add(new ChildCollectionProperty(childCollSpec.getCollectionName(), childType, false));

         resultTypes.addAll(childResultTypes);
         childResultTypes.forEach(t -> typesInScope.put(t.getTypeName(), t));
      }

      return new ChildCollectionContrs(childCollectionProperties, resultTypes);
   }

   private String getOutputFieldName
      (
         TableFieldExpr tableFieldExpr,
         Field dbField
      )
   {
      return valueOrGet(tableFieldExpr.getJsonProperty(), () -> defaultPropertyNameFn.apply(dbField.getName()));
   }

   private Map<String,Field> getTableFieldsByName(RelId relId)
   {
      RelMetadata relMd = valueOrThrow(dbmd.getRelationMetadata(relId), () ->
         new RuntimeException("Metadata for table " + relId + " not found.")
      );

      return relMd.getFields().stream().collect(toMap(Field::getName, identity()));
   }

   private @Nullable ResultType findTypeIgnoringNameExtensions
      (
         ResultType typeToFind,
         Map<String, ResultType> inMap
      )
   {
      String baseName = typeToFind.getTypeName();

      for ( Map.Entry<String, ResultType> entry: inMap.entrySet() )
      {
         boolean baseNamesMatch =
            entry.getKey().startsWith(baseName) &&
            (entry.getKey().equals(baseName) ||
             entry.getKey().charAt(baseName.length()) == '_'); // underscore used as suffix separator for making unique names

         if ( baseNamesMatch && typeToFind.equalsIgnoringName(entry.getValue()) )
            return entry.getValue();
      }

      return null;
   }

   private boolean noFkFieldKnownNotNullable(RelId childRelId, ParentSpec parentSpec)
   {
      RelId parentRelId = dbmd.toRelId(parentSpec.getParentTableJsonSpec().getTable(), defaultSchema);
      @Nullable Set<String> specFkFields = parentSpec.getChildForeignKeyFieldsSet();
      ForeignKey fk = valueOrThrow(dbmd.getForeignKeyFromTo(childRelId, parentRelId, specFkFields, ForeignKeyScope.REGISTERED_TABLES_ONLY), () ->
         new RuntimeException("foreign key to parent not found")
      );

      Map<String,Field> childFieldsByName = getTableFieldsByName(childRelId);

      for ( String fkFieldName : fk.getSourceFieldNames() )
      {
         Field fkField = valueOrThrow(childFieldsByName.get(fkFieldName), () ->
            new RuntimeException("foreign key not found")
         );

         if ( !valueOr(fkField.getNullable(), true) )
            return false;
      }

      return true;
   }
}

class InlineParentContrs
{
   ResultTypeBuilder typeBuilder;
   List<ResultType> resultTypes;

   InlineParentContrs
      (
         ResultTypeBuilder typeBuilder,
         List<ResultType> resultTypes
      )
   {
      this.typeBuilder = typeBuilder;
      this.resultTypes = resultTypes;
   }
}

class RefdParentContrs
{
   List<ParentReferenceProperty> parentReferenceProperties;
   List<ResultType> resultTypes;

   RefdParentContrs
      (
         List<ParentReferenceProperty> parentReferenceProperties,
         List<ResultType> resultTypes
      )
   {
      this.parentReferenceProperties = parentReferenceProperties;
      this.resultTypes = resultTypes;
   }
}

class ChildCollectionContrs
{
   List<ChildCollectionProperty> childCollectionProperties;
   List<ResultType> resultTypes;

   ChildCollectionContrs
      (
         List<ChildCollectionProperty> childCollectionProperties,
         List<ResultType> resultTypes
      )
   {
      this.childCollectionProperties = childCollectionProperties;
      this.resultTypes = resultTypes;
   }
}

