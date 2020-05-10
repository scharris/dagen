package org.sqljson.util;

import java.util.List;
import java.util.Set;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import org.checkerframework.checker.nullness.qual.Nullable;

import org.sqljson.StatementSpecificationException;
import org.sqljson.dbmd.DatabaseMetadata;
import org.sqljson.dbmd.Field;
import org.sqljson.dbmd.RelId;
import org.sqljson.dbmd.RelMetadata;


public final class StatementValidations
{
   public static RelId identifySpecificationTable
      (
         String table, // as from input, possibly qualified
         @Nullable String defaultSchema,
         DatabaseMetadata dbmd,
         String statementsSource,
         String statementName,
         String statementPart
      )
      throws StatementSpecificationException
   {
      @Nullable RelMetadata relMd = dbmd.getRelationMetadata(dbmd.toRelId(table, defaultSchema));

      if ( relMd == null )
         throw new StatementSpecificationException(
            statementsSource,
            statementName,
            statementPart,
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
         String statementName,
         String statementPart
      )
      throws StatementSpecificationException
   {
      @Nullable RelMetadata relMd = dbmd.getRelationMetadata(dbmd.toRelId(table, defaultSchema));

      if ( relMd == null )
         throw new StatementSpecificationException(
            statementsSource,
            statementName,
            statementPart,
            "Table '" + table + "' was not found in database metadata."
         );

      verifyTableFieldsExist(fieldNames, relMd, dbmd, statementsSource, statementName, statementPart);
   }

   public static void verifyTableFieldsExist
      (
         List<String> fieldNames,
         RelMetadata relMd,
         DatabaseMetadata dbmd,
         String statementsSource,
         String statementName,
         String statementPart
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
            statementName,
            statementPart,
            "Field(s) not found in table " + relMd.getRelationId() + ": " + missingFields + "."
         );
   }

   private StatementValidations() {}
}

