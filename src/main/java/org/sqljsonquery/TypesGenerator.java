package org.sqljsonquery;

import java.util.*;
import static java.util.stream.Collectors.*;
import static java.util.function.Function.identity;

import gov.fda.nctr.dbmd.*;

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

   public List<GeneratedType> makeResultTypeDefinitions(TableOutputSpec tos)
   {
      RelId relId = dbmd.identifyTable(tos.getTable(), defaultSchema);
      RelMetadata relMd = dbmd.getRelationMetadata(relId).orElseThrow(() ->
         new RuntimeException("Metadata for table " + relId + " not found.")
      );
      Map<String,Field> dbFieldsByName = relMd.getFields().stream().collect(toMap(Field::getName, identity()));

      // TODO: Ensure that the class name is unique here across generated types, maybe put in subpackage by query name.
      String className = camelCase(tos.getTable());

      GeneratedTypeBuilder typeBuilder = new GeneratedTypeBuilder(className);

      // Add this table's own directly contained database fields to the generated type.
      for ( TableOutputField tof : tos.getTableOutputFields() )
      {
         Field dbField = dbFieldsByName.get(dbmd.normalizeName(tof.getDatabaseFieldName()));
         if ( dbField == null )
            throw new RuntimeException("no metadata for field " + tos.getTable() + "." + tof.getDatabaseFieldName());
         typeBuilder.addDatabaseField(tof.getFinalOutputFieldName(), dbField);
      }

      List<GeneratedType> generatedTypes = new ArrayList<>();

      // Add each child table's types to the overall list of generated types, and their collection fields to this type.
      for ( ChildTableSpec childTableSpec : tos.getChildTableSpecs() )
      {
         // Generate types by traversing the child table and its parents and children.
         List<GeneratedType> childGenTypes = makeResultTypeDefinitions(childTableSpec.getTableOutputSpec());
         GeneratedType childType = childGenTypes.get(0);
         generatedTypes.addAll(childGenTypes);
         typeBuilder.addChildCollectionField(childTableSpec.getCollectionFieldName(), childType);
      }

      // Add each parent table's types to the overall list of generated types, and to this type either add a reference
      // field for the type if a wrapper field is specified, or else add add its fields inline.
      for ( ParentTableSpec parentTableSpec :  tos.getParentTableSpecs() )
      {
         // Generate types by traversing the parent table and its parents and children.
         List<GeneratedType> parentGenTypes = makeResultTypeDefinitions(parentTableSpec.getTableOutputSpec());
         GeneratedType parentType = parentGenTypes.get(0);

         if ( parentTableSpec.getWrapperFieldName().isPresent() )
         {
            typeBuilder.addParentReferenceField(parentTableSpec.getWrapperFieldName().get(), parentType);
            generatedTypes.addAll(parentGenTypes);
         }
         else
         {
            typeBuilder.addDatabaseFields(parentType.getDatabaseFields());
            typeBuilder.addChildCollectionFields(parentType.getChildCollectionFields());
            typeBuilder.addParentReferenceFields(parentType.getParentReferenceFields());
            // An inline/unwrapped parent needs no type generated for the parent table itself.
            generatedTypes.addAll(parentGenTypes.subList(1, parentGenTypes.size()));
         }
      }

      generatedTypes.add(0, typeBuilder.build()); // The tos's top table type must be at the head of the returned list.

      return generatedTypes;
   }

}
