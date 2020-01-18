package org.sqljson;

import java.util.*;
import java.util.function.Function;
import static java.util.Optional.empty;
import static java.util.stream.Collectors.*;
import static java.util.function.Function.identity;
import static org.sqljson.util.Optionals.opt;

import org.sqljson.dbmd.*;
import org.sqljson.result_types.ExpressionField;
import org.sqljson.result_types.GeneratedType;
import org.sqljson.result_types.GeneratedTypeBuilder;
import org.sqljson.specs.queries.*;
import org.sqljson.util.StringFuns;


class QueryTypesGenerator
{
   private final DatabaseMetadata dbmd;
   private final Optional<String> defaultSchema;

   QueryTypesGenerator
   (
      DatabaseMetadata dbmd,
      Optional<String> defaultSchema
   )
   {
      this.dbmd = dbmd;
      this.defaultSchema = defaultSchema;
   }

   List<GeneratedType> generateTypes
   (
      TableJsonSpec tjs,
      Map<String,GeneratedType> previouslyGeneratedTypesByName,
      Function<String,String> outputFieldNameDefaultFn
   )
   {
      List<GeneratedType> generatedTypes = new ArrayList<>();
      Map<String,GeneratedType> typesInScope = new HashMap<>(previouslyGeneratedTypesByName);

      GeneratedTypeBuilder typeBuilder = new GeneratedTypeBuilder();

      RelId relId = dbmd.identifyTable(tjs.getTable(), defaultSchema);
      Map<String, Field> dbFieldsByName = getTableFieldsByName(relId);

      // Add this table's own directly contained database fields to the generated type.
      for ( TableFieldExpr tfe : tjs.getFieldExpressions() )
      {
         if ( tfe.isSimpleField() )
         {
            Field dbField = dbFieldsByName.get(dbmd.normalizeName(tfe.getField()));
            if ( dbField == null )
               throw new RuntimeException("no metadata for field " + tjs.getTable() + "." + tfe.getField());
            typeBuilder.addDatabaseField(getOutputFieldName(tfe, dbField, outputFieldNameDefaultFn), dbField, tfe.getGenerateTypes());
         }
         else
         {
            String jsonProperty = tfe.getJsonProperty().orElseThrow(() ->
                new RuntimeException("Expression field " + tjs.getTable() + "." + tfe + " requires a json property.")
            );
            typeBuilder.addExpressionField(
                new ExpressionField(jsonProperty, opt(tfe.getExpression()), tfe.getGenerateTypes())
            );
         }
      }

      // Add fields from inline parents, but do not add their top-level types to the generated types results.
      for ( InlineParentSpec inlineParentTableSpec :  tjs.getInlineParentTables() )
      {
         // Generate types by traversing the parent table and its parents and children.
         List<GeneratedType> parentGenTypes =
            generateTypes(inlineParentTableSpec.getParentTableJsonSpec(), typesInScope, outputFieldNameDefaultFn);
         GeneratedType parentType = parentGenTypes.get(0); // will not be generated

         // If the parent record might be absent, then all inline fields must be nullable.
         boolean forceNullable = inlineParentTableSpec.getParentTableJsonSpec().hasCondition() || // parent has condition
                                 noFkFieldKnownNotNullable(relId, inlineParentTableSpec);           // fk nullable

         typeBuilder.addAllFieldsFrom(parentType, forceNullable);

         List<GeneratedType> actualGenParentTypes = parentGenTypes.subList(1, parentGenTypes.size());
         generatedTypes.addAll(actualGenParentTypes);
         actualGenParentTypes.forEach(t -> typesInScope.put(t.getTypeName(), t));
      }

      // Add reference fields for referenced parents, and add their generated types to the generated types results.
      for ( ReferencedParentSpec parentTableSpec : tjs.getReferencedParentTables() )
      {
         // Generate types by traversing the parent table and its parents and children.
         List<GeneratedType> parentGenTypes =
            generateTypes(parentTableSpec.getParentTableJsonSpec(), typesInScope, outputFieldNameDefaultFn);
         GeneratedType parentType = parentGenTypes.get(0);

         boolean nullable = parentTableSpec.getParentTableJsonSpec().hasCondition() || // parent has condition
                            noFkFieldKnownNotNullable(relId, parentTableSpec);           // fk nullable

         typeBuilder.addParentReferenceField(parentTableSpec.getReferenceName(), parentType, nullable);

         generatedTypes.addAll(parentGenTypes);
         parentGenTypes.forEach(t -> typesInScope.put(t.getTypeName(), t));
      }

      // Add each child table's types to the overall list of generated types, and their collection fields to this type.
      for ( ChildCollectionSpec childCollSpec : tjs.getChildTableCollections() )
      {
         // Generate types by traversing the child table and its parents and children.
         List<GeneratedType> childGenTypes =
             generateTypes(
                 childCollSpec.getTableJson(),
                 typesInScope,
                 outputFieldNameDefaultFn
             );

         // Mark the top-level child type as unwrapped if specified.
         GeneratedType childType = childGenTypes.get(0).withUnwrapped(childCollSpec.getUnwrap());
         childGenTypes.set(0, childType);

         typeBuilder.addChildCollectionField(childCollSpec.getCollectionName(), childType, false);

         generatedTypes.addAll(childGenTypes);
         childGenTypes.forEach(t -> typesInScope.put(t.getTypeName(), t));
      }

      // Finally the top table's type must be added at leading position in the returned list. But if the type is
      // essentially identical to one already in scope, then add the previously generated instance instead.
      String baseTypeName = StringFuns.upperCamelCase(tjs.getTable()); // Base type name is the desired name, without any trailing digits.
      if ( !typesInScope.containsKey(baseTypeName) ) // No previously generated type of same base name.
         generatedTypes.add(0, typeBuilder.build(baseTypeName));
      else
      {
         Optional<GeneratedType> existingIdenticalType =
            findTypeIgnoringNameExtensions(typeBuilder.build(baseTypeName), previouslyGeneratedTypesByName);
         if ( existingIdenticalType.isPresent() ) // Identical previously generated type found, use it as top type.
            generatedTypes.add(0, existingIdenticalType.get());
         else // This type does not match any previously generated, but needs a new name.
         {
            String uniqueName = StringFuns.makeNameNotInSet(baseTypeName, typesInScope.keySet(), "_");
            generatedTypes.add(0, typeBuilder.build(uniqueName));
         }
      }

      return generatedTypes;
   }

   private String getOutputFieldName
   (
      TableFieldExpr tableFieldExpr,
      Field dbField,
      Function<String,String> outputFieldNameDefaultFn
   )
   {
      return tableFieldExpr.getJsonProperty().orElseGet(() -> outputFieldNameDefaultFn.apply(dbField.getName()));
   }

   private Map<String,Field> getTableFieldsByName(RelId relId)
   {
      RelMetadata relMd = dbmd.getRelationMetadata(relId).orElseThrow(() ->
         new RuntimeException("Metadata for table " + relId + " not found.")
      );

      return relMd.getFields().stream().collect(toMap(Field::getName, identity()));
   }

   private Optional<GeneratedType> findTypeIgnoringNameExtensions
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
            return opt(entry.getValue());
      }

      return empty();
   }


   private boolean noFkFieldKnownNotNullable(RelId childRelId, ParentSpec parentSpec)
   {
      RelId parentRelId = dbmd.identifyTable(parentSpec.getParentTableJsonSpec().getTable(), defaultSchema);
      Optional<Set<String>> specFkFields = parentSpec.getChildForeignKeyFieldsSet();
      ForeignKey fk = dbmd.getForeignKeyFromTo(childRelId, parentRelId, specFkFields, ForeignKeyScope.REGISTERED_TABLES_ONLY).orElseThrow(
         () -> new RuntimeException("foreign key to parent not found")
      );

      Map<String,Field> childFieldsByName = getTableFieldsByName(childRelId);

      for ( String fkFieldName : fk.getSourceFieldNames() )
      {
         Field fkField = childFieldsByName.get(fkFieldName);
         if ( fkField == null )
            throw new RuntimeException("foreign key not found");

         if ( !(fkField.getNullable().orElse(true)) )
            return false;
      }

      return true;
   }

}
