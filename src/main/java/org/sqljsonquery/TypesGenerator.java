package org.sqljsonquery;

import java.util.*;
import static java.util.stream.Collectors.*;
import static java.util.function.Function.identity;

import gov.fda.nctr.dbmd.*;
import static gov.fda.nctr.dbmd.DBMD.ForeignKeyScope.REGISTERED_TABLES_ONLY;

import static org.sqljsonquery.util.StringFuns.*;
import org.sqljsonquery.spec.TableOutputSpec;
import org.sqljsonquery.spec.TableOutputField;
import org.sqljsonquery.spec.ChildTableSpec;
import org.sqljsonquery.spec.ParentTableSpec;
import org.sqljsonquery.types.GeneratedType;
import org.sqljsonquery.types.GeneratedTypeBuilder;


public class TypesGenerator
{
   private final DBMD dbmd;
   private final Optional<String> defaultSchema;


   public TypesGenerator
   (
      DBMD dbmd,
      Optional<String> defaultSchema
   )
   {
      this.dbmd = dbmd;
      this.defaultSchema = defaultSchema;
   }

   public List<GeneratedType> generateTypes(TableOutputSpec tos, Set<String> typeNamesInScope)
   {
      List<GeneratedType> generatedTypes = new ArrayList<>();
      Set<String> avoidTypeNames = new HashSet<>(typeNamesInScope);

      String typeName = makeNameNotInSet(camelCase(tos.getTable()), avoidTypeNames);
      avoidTypeNames.add(typeName);

      GeneratedTypeBuilder typeBuilder = new GeneratedTypeBuilder(typeName);

      RelId relId = dbmd.identifyTable(tos.getTable(), defaultSchema);
      RelMetadata relMd = dbmd.getRelationMetadata(relId).orElseThrow(() ->
         new RuntimeException("Metadata for table " + relId + " not found.")
      );
      Map<String,Field> dbFieldsByName = relMd.getFields().stream().collect(toMap(Field::getName, identity()));

      // Add this table's own directly contained database fields to the generated type.
      for ( TableOutputField tof : tos.getTableOutputFields() )
      {
         Field dbField = dbFieldsByName.get(dbmd.normalizeName(tof.getDatabaseFieldName()));
         if ( dbField == null )
            throw new RuntimeException("no metadata for field " + tos.getTable() + "." + tof.getDatabaseFieldName());

         typeBuilder.addDatabaseField(tof.getFinalOutputFieldName(), dbField);
      }

      // Add each child table's types to the overall list of generated types, and their collection fields to this type.
      for ( ChildTableSpec childTableSpec : tos.getChildTableSpecs() )
      {
         // Generate types by traversing the child table and its parents and children.
         List<GeneratedType> childGenTypes = generateTypes(childTableSpec.getTableOutputSpec(), avoidTypeNames);
         GeneratedType childType = childGenTypes.get(0);

         typeBuilder.addChildCollectionField(childTableSpec.getCollectionFieldName(), childType);

         generatedTypes.addAll(childGenTypes);
         childGenTypes.forEach(t -> avoidTypeNames.add(t.getTypeName()));
      }

      // Add each parent table's types to the overall list of generated types, and to this type either add a reference
      // field for the type if a wrapper field is specified, or else add add its fields inline.
      for ( ParentTableSpec parentTableSpec :  tos.getParentTableSpecs() )
      {
         // Generate types by traversing the parent table and its parents and children.
         List<GeneratedType> parentGenTypes = generateTypes(parentTableSpec.getTableOutputSpec(), avoidTypeNames);
         GeneratedType parentType = parentGenTypes.get(0); // (may not be actually generated)

         if ( parentTableSpec.getWrapperFieldName().isPresent() )
         {
            boolean nullableFk = !someFkFieldKnownNotNullable(relId, parentTableSpec);
            typeBuilder.addParentReferenceField(parentTableSpec.getWrapperFieldName().get(), parentType, nullableFk);

            generatedTypes.addAll(parentGenTypes);
            parentGenTypes.forEach(t -> avoidTypeNames.add(t.getTypeName()));
         }
         else
         {  // An inline/unwrapped parent has no type generated for the parent top table, we just include its fields.
            typeBuilder.addDatabaseFields(parentType.getDatabaseFields());
            typeBuilder.addChildCollectionFields(parentType.getChildCollectionFields());
            typeBuilder.addParentReferenceFields(parentType.getParentReferenceFields());

            List<GeneratedType> actualGenParentTypes = parentGenTypes.subList(1, parentGenTypes.size());
            generatedTypes.addAll(actualGenParentTypes);
            actualGenParentTypes.forEach(t -> avoidTypeNames.add(t.getTypeName()));
         }
      }

      generatedTypes.add(0, typeBuilder.build()); // The tos's top table type must be at the head of the returned list.

      return generatedTypes;
   }

   private boolean someFkFieldKnownNotNullable(RelId childRelId, ParentTableSpec parentTableSpec)
   {
      RelId parentRelId = dbmd.identifyTable(parentTableSpec.getTableOutputSpec().getTable(), defaultSchema);
      Optional<Set<String>> specFkFields = parentTableSpec.getChildForeignKeyFieldsSet();
      ForeignKey fk = dbmd.getForeignKeyFromTo(childRelId, parentRelId, specFkFields, REGISTERED_TABLES_ONLY).orElseThrow(
         () -> new RuntimeException("foreign key to parent not found")
      );

      RelMetadata childRelMd = dbmd.getRelationMetadata(childRelId).orElseThrow(
         () -> new RuntimeException("child table metadata not found")
      );

      List<String> fkFieldNames = fk.getSourceFieldNames();

      return
         childRelMd.getFields().stream()
         .anyMatch(f -> fkFieldNames.contains(f.getName()) && !(f.getNullable().orElse(true)));
   }

}
