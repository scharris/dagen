package org.sqljson.common.util;

import java.util.List;
import java.util.Set;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import org.checkerframework.checker.nullness.qual.Nullable;

import org.sqljson.common.StatementLocation;
import org.sqljson.common.StatementSpecificationException;
import org.sqljson.dbmd.DatabaseMetadata;
import org.sqljson.dbmd.Field;
import org.sqljson.dbmd.RelId;
import org.sqljson.dbmd.RelMetadata;
import org.sqljson.queries.specs.CustomJoinCondition;
import org.sqljson.queries.specs.TableJsonSpec;


public final class StatementValidations
{
   public static RelId identifySpecificationTable
      (
         String table, // as from input, possibly qualified
         @Nullable String defaultSchema,
         DatabaseMetadata dbmd,
         String statementsSource,
         StatementLocation statementLocation
      )
      throws StatementSpecificationException
   {
      @Nullable RelMetadata relMd = dbmd.getRelationMetadata(dbmd.toRelId(table, defaultSchema));

      if ( relMd == null )
         throw new StatementSpecificationException(
            statementsSource,
            statementLocation,
            "Table '" + table + "' was not found in database metadata."
         );

      return relMd.getRelationId();
   }

   public static void verifyTableFieldsExist
      (
         String table, // maybe qualified
         @Nullable String defaultSchema,
         List<String> fieldNames,
         DatabaseMetadata dbmd,
         String statementsSource,
         StatementLocation statementLocation
      )
      throws StatementSpecificationException
   {
      @Nullable RelMetadata relMd = dbmd.getRelationMetadata(dbmd.toRelId(table, defaultSchema));

      if ( relMd == null )
         throw new StatementSpecificationException(
            statementsSource,
            statementLocation,
            "Table '" + table + "' was not found in database metadata."
         );

      verifyTableFieldsExist(fieldNames, relMd, dbmd, statementsSource, statementLocation);
   }

   public static void verifyTableFieldsExist
      (
         List<String> fieldNames,
         RelMetadata relMd,
         DatabaseMetadata dbmd,
         String statementsSource,
         StatementLocation statementLocation
      )
      throws StatementSpecificationException
   {
      Set<String> dbmdTableFields = relMd.getFields().stream().map(Field::getName).collect(toSet());

      List<String> missingFields =
         fieldNames.stream()
         .filter(fieldName -> !dbmdTableFields.contains(dbmd.normalizeName(fieldName)))
         .collect(toList());

      if ( !missingFields.isEmpty() )
         throw new StatementSpecificationException(
            statementsSource,
            statementLocation,
            "Field(s) not found in table " + relMd.getRelationId() + ": " + missingFields + "."
         );
   }

   public static void verifySimpleSelectFieldsExist
      (
         TableJsonSpec tableSpec,
         @Nullable String defaultSchema,
         DatabaseMetadata dbmd,
         String statementsSource,
         StatementLocation stmtLoc
      )
   {
      List<String> simpleSelectFields =
         tableSpec.getFieldExpressionsList().stream()
         .filter(tfe -> tfe.getField() != null)
         .map(tfe -> requireNonNull(tfe.getField()))
         .collect(toList());

      verifyTableFieldsExist(
         tableSpec.getTable(),
         defaultSchema,
         simpleSelectFields,
         dbmd,
         statementsSource,
         stmtLoc
      );
   }

   public static void validateCustomJoinCondition
      (
         CustomJoinCondition customJoinCond,
         RelId childRelId,
         RelId parentRelId,
         DatabaseMetadata dbmd,
         String stmtsSource,
         StatementLocation stmtLoc
      )
      throws StatementSpecificationException
   {
      @Nullable RelMetadata parentMd = dbmd.getRelationMetadata(parentRelId);
      @Nullable RelMetadata childMd = dbmd.getRelationMetadata(childRelId);

      if ( parentMd == null )
         throw new StatementSpecificationException(stmtsSource,
            stmtLoc.withPart("custom join condition"), "Parent table not found."
         );
      if ( childMd == null )
         throw new StatementSpecificationException(stmtsSource,
            stmtLoc.withPart("custom join condition"), "Child table not found."
         );

      List<String> parentMatchFields =
         customJoinCond.getEquatedFields().stream()
            .map(CustomJoinCondition.FieldPair::getParentPrimaryKeyField)
            .collect(toList());

      verifyTableFieldsExist(parentMatchFields, parentMd, dbmd, stmtsSource, stmtLoc);

      List<String> childMatchFields =
         customJoinCond.getEquatedFields().stream()
            .map(CustomJoinCondition.FieldPair::getChildField)
            .collect(toList());

      verifyTableFieldsExist(childMatchFields, childMd, dbmd, stmtsSource, stmtLoc);
   }

   private StatementValidations() {}
}

