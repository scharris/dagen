package org.sqljson.queries;

import java.util.List;
import java.util.Set;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import org.checkerframework.checker.nullness.qual.Nullable;

import org.sqljson.dbmd.DatabaseMetadata;
import org.sqljson.dbmd.Field;
import org.sqljson.dbmd.RelId;
import org.sqljson.dbmd.RelMetadata;
import org.sqljson.queries.specs.CustomJoinCondition;
import org.sqljson.queries.specs.SpecError;
import org.sqljson.queries.specs.SpecLocation;
import org.sqljson.queries.specs.TableJsonSpec;


public final class SpecValidations
{
   public static RelId identifyTable
      (
         String table, // as from input, possibly qualified
         @Nullable String defaultSchema,
         DatabaseMetadata dbmd,
         SpecLocation specLocation
      )
      throws SpecError
   {
      @Nullable RelMetadata relMd = dbmd.getRelationMetadata(dbmd.toRelId(table, defaultSchema));

      if ( relMd == null )
         throw new SpecError(
            specLocation,
            "Table '" + table + "' was not found in database metadata."
         );

      return relMd.getRelationId();
   }

   public static void verifySimpleSelectFieldsExist
      (
         TableJsonSpec tableSpec,
         @Nullable String defaultSchema,
         DatabaseMetadata dbmd,
         SpecLocation stmtLoc
      )
   {
      List<String> simpleSelectFields =
         tableSpec.getFieldExpressionsList().stream()
         .filter(tfe -> tfe.getField() != null)
         .map(tfe -> requireNonNull(tfe.getField()))
         .collect(toList());

      verifyTableFieldsExist(tableSpec.getTable(), defaultSchema, simpleSelectFields, dbmd, stmtLoc);
   }

   public static void validateCustomJoinCondition
      (
         CustomJoinCondition customJoinCond,
         RelId childRelId,
         RelId parentRelId,
         DatabaseMetadata dbmd,
         SpecLocation stmtLoc
      )
      throws SpecError
   {
      @Nullable RelMetadata parentMd = dbmd.getRelationMetadata(parentRelId);
      @Nullable RelMetadata childMd = dbmd.getRelationMetadata(childRelId);

      if ( parentMd == null )
         throw new SpecError(stmtLoc.withPart("custom join condition"), "Parent table not found.");
      if ( childMd == null )
         throw new SpecError(stmtLoc.withPart("custom join condition"), "Child table not found.");

      List<String> parentMatchFields =
         customJoinCond.getEquatedFields().stream()
         .map(CustomJoinCondition.FieldPair::getParentPrimaryKeyField)
         .collect(toList());

      verifyTableFieldsExist(parentMd, parentMatchFields, dbmd, stmtLoc);

      List<String> childMatchFields =
         customJoinCond.getEquatedFields().stream()
         .map(CustomJoinCondition.FieldPair::getChildField)
         .collect(toList());

      verifyTableFieldsExist(childMd, childMatchFields, dbmd, stmtLoc);
   }

   private static void verifyTableFieldsExist
      (
         String table, // maybe qualified
         @Nullable String defaultSchema,
         List<String> fieldNames,
         DatabaseMetadata dbmd,
         SpecLocation specLocation
      )
      throws SpecError
   {
      @Nullable RelMetadata relMd = dbmd.getRelationMetadata(dbmd.toRelId(table, defaultSchema));

      if ( relMd == null )
         throw new SpecError(specLocation, "Table '" + table + "' was not found in database metadata.");

      verifyTableFieldsExist(relMd, fieldNames, dbmd, specLocation);
   }

   private static void verifyTableFieldsExist
      (
         RelMetadata relMd,
         List<String> fieldNames,
         DatabaseMetadata dbmd,
         SpecLocation specLocation
      )
      throws SpecError
   {
      Set<String> dbmdTableFields = relMd.getFields().stream().map(Field::getName).collect(toSet());

      List<String> missingFields =
         fieldNames.stream()
            .filter(fieldName -> !dbmdTableFields.contains(dbmd.normalizeName(fieldName)))
            .collect(toList());

      if ( !missingFields.isEmpty() )
         throw new SpecError(
            specLocation,
            "Field(s) not found in table " + relMd.getRelationId() + ": " + missingFields + "."
         );
   }

   private SpecValidations() {}
}

