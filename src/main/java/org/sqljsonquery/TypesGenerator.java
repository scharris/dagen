package org.sqljsonquery;

import java.util.*;
import java.util.function.Function;

import static java.util.stream.Collectors.*;
import static java.util.function.Function.identity;

import static org.sqljsonquery.util.StringFuns.*;
import org.sqljsonquery.dbmd.*;
import static org.sqljsonquery.dbmd.ForeignKeyScope.REGISTERED_TABLES_ONLY;
import org.sqljsonquery.queryspec.*;
import org.sqljsonquery.types.GeneratedType;
import org.sqljsonquery.types.GeneratedTypeBuilder;


public class TypesGenerator
{
   private final DatabaseMetadata dbmd;
   private final Optional<String> defaultSchema;
   private final Function<String,String> outputFieldNameDefaultFn;

   public TypesGenerator
   (
      DatabaseMetadata dbmd,
      Optional<String> defaultSchema,
      Function<String,String> outputFieldNameDefaultFn
   )
   {
      this.dbmd = dbmd;
      this.defaultSchema = defaultSchema;
      this.outputFieldNameDefaultFn = outputFieldNameDefaultFn;
   }

   public List<GeneratedType> generateTypes
   (
      TableOutputSpec tos,
      Set<String> typeNamesInScope
   )
   {
      List<GeneratedType> generatedTypes = new ArrayList<>();
      Set<String> avoidTypeNames = new HashSet<>(typeNamesInScope);

      String typeName = makeNameNotInSet(upperCamelCase(tos.getTableName()), avoidTypeNames);
      avoidTypeNames.add(typeName);

      GeneratedTypeBuilder typeBuilder = new GeneratedTypeBuilder(typeName);

      RelId relId = dbmd.identifyTable(tos.getTableName(), defaultSchema);
      Map<String,Field> dbFieldsByName = getTableFieldsByName(relId);

      // Add this table's own directly contained database fields to the generated type.
      for ( TableOutputField tof : tos.getNativeFields() )
      {
         Field dbField = dbFieldsByName.get(dbmd.normalizeName(tof.getDatabaseFieldName()));
         if ( dbField == null )
            throw new RuntimeException("no metadata for field " + tos.getTableName() + "." + tof.getDatabaseFieldName());
         typeBuilder.addDatabaseField(getOutputFieldName(tof, dbField), dbField);
      }

      // Add fields from inline parents, but do not add their top-level types to the generated types results.
      for ( InlineParentSpec inlineParentTableSpec :  tos.getInlineParents() )
      {
         // Generate types by traversing the parent table and its parents and children.
         List<GeneratedType> parentGenTypes = generateTypes(inlineParentTableSpec.getParentTableOutputSpec(), avoidTypeNames);
         GeneratedType parentType = parentGenTypes.get(0); // will not be generated

         // If the parent record might be absent, then all inline fields must be nullable.
         boolean forceNullable = inlineParentTableSpec.getParentTableOutputSpec().getFilter().isPresent() || // parent has filter
                                 noFkFieldKnownNotNullable(relId, inlineParentTableSpec);                    // fk nullable

         typeBuilder.addAllFieldsFrom(parentType, forceNullable);

         List<GeneratedType> actualGenParentTypes = parentGenTypes.subList(1, parentGenTypes.size());
         generatedTypes.addAll(actualGenParentTypes);
         actualGenParentTypes.forEach(t -> avoidTypeNames.add(t.getTypeName()));
      }

      // Add reference fields for referenced parents, and add their generated types to the generated types results.
      for ( ReferencedParentSpec parentTableSpec : tos.getReferencedParents() )
      {
         // Generate types by traversing the parent table and its parents and children.
         List<GeneratedType> parentGenTypes = generateTypes(parentTableSpec.getParentTableOutputSpec(), avoidTypeNames);
         GeneratedType parentType = parentGenTypes.get(0);

         boolean nullable = parentTableSpec.getParentTableOutputSpec().getFilter().isPresent() || // parent has filter
                            noFkFieldKnownNotNullable(relId, parentTableSpec);                    // fk nullable

         typeBuilder.addParentReferenceField(parentTableSpec.getReferenceFieldName(), parentType, nullable);

         generatedTypes.addAll(parentGenTypes);
         parentGenTypes.forEach(t -> avoidTypeNames.add(t.getTypeName()));
      }

      // Add each child table's types to the overall list of generated types, and their collection fields to this type.
      for ( ChildCollectionSpec childCollectionSpec : tos.getChildCollections() )
      {
         // Generate types by traversing the child table and its parents and children.
         List<GeneratedType> childGenTypes =
            generateTypes(
               childCollectionSpec.getChildTableOutputSpec(),
               avoidTypeNames
            );
         GeneratedType childType = childGenTypes.get(0);

         typeBuilder.addChildCollectionField(childCollectionSpec.getChildCollectionName(), childType, false);

         generatedTypes.addAll(childGenTypes);
         childGenTypes.forEach(t -> avoidTypeNames.add(t.getTypeName()));
      }


      generatedTypes.add(0, typeBuilder.build()); // The tos's top table type must be at the head of the returned list.

      return generatedTypes;
   }

   private String getOutputFieldName(TableOutputField tof, Field dbField)
   {
      return tof.getOutputName().orElseGet(() -> outputFieldNameDefaultFn.apply(dbField.getName()));
   }

   private Map<String,Field> getTableFieldsByName(RelId relId)
   {
      RelMetadata relMd = dbmd.getRelationMetadata(relId).orElseThrow(() ->
         new RuntimeException("Metadata for table " + relId + " not found.")
      );

      return relMd.getFields().stream().collect(toMap(Field::getName, identity()));
   }

   private boolean noFkFieldKnownNotNullable(RelId childRelId, ParentSpec parentSpec)
   {
      RelId parentRelId = dbmd.identifyTable(parentSpec.getParentTableOutputSpec().getTableName(), defaultSchema);
      Optional<Set<String>> specFkFields = parentSpec.getChildForeignKeyFieldsSet();
      ForeignKey fk = dbmd.getForeignKeyFromTo(childRelId, parentRelId, specFkFields, REGISTERED_TABLES_ONLY).orElseThrow(
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
