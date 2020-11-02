package org.sqljson.queries;

import java.util.*;
import java.util.function.Function;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.*;
import static java.util.function.Function.identity;
import static org.sqljson.common.util.Nullables.*;

import org.checkerframework.checker.nullness.qual.Nullable;

import org.sqljson.dbmd.*;
import org.sqljson.queries.result_types.GeneratedType;
import org.sqljson.queries.result_types.GeneratedTypeBuilder;
import org.sqljson.queries.specs.*;
import org.sqljson.common.util.StringFuns;


class QueryTypesGenerator
{
   private final DatabaseMetadata dbmd;
   private final @Nullable String defaultSchema;
   private final Function<String,String> outputFieldNameDefaultFn;

   QueryTypesGenerator
      (
         DatabaseMetadata dbmd,
         @Nullable String defaultSchema,
         Function<String,String> outputFieldNameDefaultFn
      )
   {
      this.dbmd = dbmd;
      this.defaultSchema = defaultSchema;
      this.outputFieldNameDefaultFn = outputFieldNameDefaultFn;
   }

   @SuppressWarnings("keyfor")
   List<GeneratedType> generateTypes
      (
         TableJsonSpec tjs,
         Map<String,GeneratedType> previouslyGeneratedTypesByName
      )
   {
      Map<String,GeneratedType> typesInScope = new HashMap<>(previouslyGeneratedTypesByName);

      GeneratedTypeBuilder typeBuilder = new GeneratedTypeBuilder();
      List<GeneratedType> generatedTypes = new ArrayList<>();

      RelId relId = dbmd.toRelId(tjs.getTable(), defaultSchema);
      Map<String, Field> dbFieldsByName = getTableFieldsByName(relId);

      // Add this table's own directly contained database fields to the generated type.
      typeBuilder.addAllFieldsFrom(buildExpressionFields(relId, tjs.getFieldExpressionsList(), dbFieldsByName));

      InlineParentsPart inlineParentsPart = buildInlineParentsPart(relId, tjs.getInlineParentTablesList(), typesInScope);
      typeBuilder.addAllFieldsFrom(inlineParentsPart.typesBuilder);
      generatedTypes.addAll(inlineParentsPart.generatedTypes);
      inlineParentsPart.generatedTypes.forEach(t -> typesInScope.put(t.getTypeName(), t));

      ReferencedParentsPart refdParentsPart = buildReferencedParentsPart(relId, tjs.getReferencedParentTablesList(), typesInScope);
      typeBuilder.addAllFieldsFrom(refdParentsPart.typesBuilder);
      generatedTypes.addAll(refdParentsPart.generatedTypes);
      inlineParentsPart.generatedTypes.forEach(t -> typesInScope.put(t.getTypeName(), t));

      ChildCollectionsPart childCollsPart = buildChildCollectionsPart(tjs.getChildTableCollectionsList(), typesInScope);
      typeBuilder.addAllFieldsFrom(childCollsPart.typesBuilder);
      generatedTypes.addAll(childCollsPart.generatedTypes);
      inlineParentsPart.generatedTypes.forEach(t -> typesInScope.put(t.getTypeName(), t));

      // Finally the top table's type must be added at leading position in the returned list. But if the type is
      // essentially identical to one already in scope (ignoring only any name extension added to make the name unique),
      // then add the previously generated instance instead.
      String baseTypeName = StringFuns.upperCamelCase(tjs.getTable()); // Base type name is the desired name, without any trailing digits.
      if ( !typesInScope.containsKey(baseTypeName) ) // No previously generated type of same base name.
         generatedTypes.add(0, typeBuilder.build(baseTypeName));
      else
      {
         @Nullable GeneratedType existingIdenticalType =
            findTypeIgnoringNameExtensions(typeBuilder.build(baseTypeName), previouslyGeneratedTypesByName);
         if ( existingIdenticalType != null ) // Identical previously generated type found, use it as top type.
            generatedTypes.add(0, existingIdenticalType);
         else // This type does not match any previously generated, but needs a new name.
         {
            String uniqueName = StringFuns.makeNameNotInSet(baseTypeName, typesInScope.keySet(), "_");
            generatedTypes.add(0, typeBuilder.build(uniqueName));
         }
      }

      return generatedTypes;
   }

   private GeneratedTypeBuilder buildExpressionFields
      (
         RelId relId,
         List<TableFieldExpr> tableFieldExpressions,
         Map<String,Field> dbFieldsByName
      )
   {
      GeneratedTypeBuilder typeBuilder = new GeneratedTypeBuilder();

      for ( TableFieldExpr tfe : tableFieldExpressions )
      {
         if ( tfe.getField() != null )
         {
            Field dbField = requireNonNull(dbFieldsByName.get(dbmd.normalizeName(requireNonNull(tfe.getField()))),
                           "no metadata for field " + relId + "." + tfe.getField());
            typeBuilder.addDatabaseField(getOutputFieldName(tfe, dbField), dbField, tfe.getGeneratedFieldType());
         }
         else
         {
            assert tfe.getExpression() != null;
            String jsonProperty = valueOrThrow(tfe.getJsonProperty(), () ->
                new RuntimeException("Expression field " + relId + "." + tfe + " requires a json property.")
            );
            typeBuilder.addExpressionField(jsonProperty, tfe.getExpression(), tfe.getGeneratedFieldType());
         }
      }

      return typeBuilder;
   }

   // Build the inline parents part of the generated type.
   private InlineParentsPart buildInlineParentsPart
      (
         RelId relId,
         List<InlineParentSpec> inlineParentSpecs,
         Map<String,GeneratedType> envTypesInScope
      )
   {
      Map<String,GeneratedType> typesInScope = new HashMap<>(envTypesInScope);

      GeneratedTypeBuilder typeBuilder = new GeneratedTypeBuilder();
      List<GeneratedType> generatedTypes = new ArrayList<>();

      for ( InlineParentSpec inlineParentTableSpec :  inlineParentSpecs )
      {
         // Generate types by traversing the parent table and its parents and children.
         List<GeneratedType> parentGenTypes = generateTypes(inlineParentTableSpec.getParentTableJsonSpec(), typesInScope);
         GeneratedType parentType = parentGenTypes.get(0); // will not be generated

         // If the parent record might be absent, then all inline fields must be nullable.
         boolean forceNullable = inlineParentTableSpec.getParentTableJsonSpec().hasCondition() || // parent has condition
             noFkFieldKnownNotNullable(relId, inlineParentTableSpec);         // fk nullable

         typeBuilder.addAllFieldsFrom(parentType, forceNullable);

         List<GeneratedType> actualGenParentTypes = parentGenTypes.subList(1, parentGenTypes.size());
         generatedTypes.addAll(actualGenParentTypes);
         actualGenParentTypes.forEach(t -> typesInScope.put(t.getTypeName(), t));
      }

      return new InlineParentsPart(typeBuilder, generatedTypes);
   }

   private ReferencedParentsPart buildReferencedParentsPart
      (
         RelId relId,
         List<ReferencedParentSpec> referencedParentSpecs,
         Map<String,GeneratedType> envTypesInScope
      )
   {
      Map<String,GeneratedType> typesInScope = new HashMap<>(envTypesInScope);

      GeneratedTypeBuilder typeBuilder = new GeneratedTypeBuilder();
      List<GeneratedType> generatedTypes = new ArrayList<>();

      for ( ReferencedParentSpec parentTableSpec : referencedParentSpecs )
      {
         // Generate types by traversing the parent table and its parents and children.
         List<GeneratedType> parentGenTypes = generateTypes(parentTableSpec.getParentTableJsonSpec(), typesInScope);
         GeneratedType parentType = parentGenTypes.get(0);

         boolean nullable = parentTableSpec.getParentTableJsonSpec().hasCondition() || // parent has condition
                            noFkFieldKnownNotNullable(relId, parentTableSpec);         // fk nullable

         typeBuilder.addParentReferenceField(parentTableSpec.getReferenceName(), parentType, nullable);

         generatedTypes.addAll(parentGenTypes);
         parentGenTypes.forEach(t -> typesInScope.put(t.getTypeName(), t));
      }

      return new ReferencedParentsPart(typeBuilder, generatedTypes);
   }

   private ChildCollectionsPart buildChildCollectionsPart
      (
         List<ChildCollectionSpec> childCollectionSpecs,
         Map<String,GeneratedType> envTypesInScope
      )
   {
      Map<String,GeneratedType> typesInScope = new HashMap<>(envTypesInScope);

      GeneratedTypeBuilder typeBuilder = new GeneratedTypeBuilder();
      List<GeneratedType> generatedTypes = new ArrayList<>();

      for ( ChildCollectionSpec childCollSpec : childCollectionSpecs )
      {
         // Generate types by traversing the child table and its parents and children.
         List<GeneratedType> childGenTypes = generateTypes(childCollSpec.getTableJson(), typesInScope);

         // Mark the top-level child type as unwrapped if specified.
         GeneratedType childType = childGenTypes.get(0).withUnwrapped(valueOr(childCollSpec.getUnwrap(), false));
         childGenTypes.set(0, childType);

         typeBuilder.addChildCollectionField(childCollSpec.getCollectionName(), childType, false);

         generatedTypes.addAll(childGenTypes);
         childGenTypes.forEach(t -> typesInScope.put(t.getTypeName(), t));
      }

      return new ChildCollectionsPart(typeBuilder, generatedTypes);
   }

   private String getOutputFieldName
      (
         TableFieldExpr tableFieldExpr,
         Field dbField
      )
   {
      return valueOrGet(tableFieldExpr.getJsonProperty(), () -> outputFieldNameDefaultFn.apply(dbField.getName()));
   }

   private Map<String,Field> getTableFieldsByName(RelId relId)
   {
      RelMetadata relMd = valueOrThrow(dbmd.getRelationMetadata(relId), () ->
         new RuntimeException("Metadata for table " + relId + " not found.")
      );

      return relMd.getFields().stream().collect(toMap(Field::getName, identity()));
   }

   private @Nullable GeneratedType findTypeIgnoringNameExtensions
      (
         GeneratedType typeToFind,
         Map<String,GeneratedType> inMap
      )
   {
      String baseName = typeToFind.getTypeName();

      for ( Map.Entry<String,GeneratedType> entry: inMap.entrySet() )
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

class InlineParentsPart
{
   GeneratedTypeBuilder typesBuilder;
   List<GeneratedType> generatedTypes;

   public InlineParentsPart
      (
         GeneratedTypeBuilder typesBuilder,
         List<GeneratedType> generatedTypes
      )
   {
      this.typesBuilder = typesBuilder;
      this.generatedTypes = generatedTypes;
   }
}

class ReferencedParentsPart
{
   GeneratedTypeBuilder typesBuilder;
   List<GeneratedType> generatedTypes;

   public ReferencedParentsPart
      (
         GeneratedTypeBuilder typesBuilder,
         List<GeneratedType> generatedTypes
      )
   {
      this.typesBuilder = typesBuilder;
      this.generatedTypes = generatedTypes;
   }
}

class ChildCollectionsPart
{
   GeneratedTypeBuilder typesBuilder;
   List<GeneratedType> generatedTypes;

   public ChildCollectionsPart
      (
         GeneratedTypeBuilder typesBuilder,
         List<GeneratedType> generatedTypes
      )
   {
      this.typesBuilder = typesBuilder;
      this.generatedTypes = generatedTypes;
   }
}

