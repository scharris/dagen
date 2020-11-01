package org.sqljson.mod_stmts;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.*;

import org.checkerframework.checker.nullness.qual.Nullable;

import org.sqljson.common.specs.StatementSpecificationException;
import org.sqljson.dbmd.DatabaseMetadata;
import org.sqljson.dbmd.RelId;
import org.sqljson.dbmd.RelMetadata;
import org.sqljson.common.specs.FieldParamCondition;
import org.sqljson.common.sql_dialects.SqlDialect;
import org.sqljson.mod_stmts.specs.ModSpec;
import org.sqljson.mod_stmts.specs.TargetField;
import org.sqljson.common.util.StringFuns;
import static org.sqljson.mod_stmts.specs.ParametersType.NAMED;
import static org.sqljson.mod_stmts.specs.ParametersType.NUMBERED;
import static org.sqljson.common.util.Nullables.*;
import static org.sqljson.common.util.StatementValidations.identifySpecificationTable;
import static org.sqljson.common.util.StatementValidations.verifyTableFieldsExist;
import static org.sqljson.common.util.StringFuns.*;


public class ModStatementGenerator
{
   private final DatabaseMetadata dbmd;
   private final SqlDialect sqlDialect;
   private final @Nullable String defaultSchema;
   private final Set<String> unqualifiedNamesSchemas;
   private final int indentSpaces;
   private final String statementsSource;

   private static final Pattern simpleNamedParamValueRegex = Pattern.compile("^:[A-Za-z][A-Za-z0-9_]*$");

   private static final String DEFAULT_TABLE_ALIAS_VAR = "$$";

   public ModStatementGenerator
      (
         DatabaseMetadata dbmd,
         @Nullable String defaultSchema,
         Set<String> unqualifiedNamesSchemas,
         String statementsSource // the source of the statements for error reporting
      )
   {
      this.dbmd = dbmd;
      this.indentSpaces = 2;
      this.sqlDialect = SqlDialect.fromDatabaseMetadata(this.dbmd, this.indentSpaces);
      this.defaultSchema = defaultSchema;
      this.unqualifiedNamesSchemas = unqualifiedNamesSchemas.stream().map(dbmd::normalizeName).collect(toSet());
      this.statementsSource = statementsSource;
   }

   public GeneratedModStatement generateModStatement
      (
         ModSpec mod
      )
   {
      switch ( mod.getCommand() )
      {
         case INSERT: return generateInsertStatement(mod);
         case UPDATE: return generateUpdateStatement(mod);
         case DELETE: return generateDeleteStatement(mod);
         default: throw new RuntimeException("Unexpected mod command " + mod.getCommand());
      }
   }

   private GeneratedModStatement generateInsertStatement
      (
         ModSpec modSpec
      )
   {
      if ( modSpec.getTableAlias() != null )
         throw specError(modSpec.getStatementName(), "tableAlias",
                        "A table alias is not allowed in an INSERT command.");
      if ( !modSpec.getFieldParamConditions().isEmpty() )
         throw specError(modSpec.getStatementName(), "fieldParamConditions",
                        "fieldParamConditions are not allowed for INSERT commands.");
      if ( modSpec.getRecordCondition() != null )
         throw specError(modSpec.getStatementName(), "recordCondition",
                        "A record condition is not allowed for INSERT commands.");

      String sql = makeInsertSql(modSpec);

      List<String> targetFieldParams = getTargetFieldParamNames(modSpec);

      return new GeneratedModStatement(modSpec, sql, targetFieldParams, emptyList());
   }

   private String makeInsertSql
      (
         ModSpec modSpec
      )
   {
      RelId relId = identifyTable(modSpec.getTable(), modSpec.getStatementName(), "insert table name");

      verifyReferencedTableFields(modSpec, relId);

      String fields = modSpec.getTargetFields().stream().map(TargetField::getField).collect(joining(",\n"));
      String fieldVals = modSpec.getTargetFields().stream() .map(TargetField::getValue).collect(joining(",\n"));

      return
         "insert into " + minimalRelIdentifier(relId) + "\n" +
            "  (\n" +
               indentLines(fields, 2 + indentSpaces) + "\n" +
            "  )\n" +
            "values\n" +
            "  (\n" +
               indentLines(fieldVals, 2 + indentSpaces) + "\n" +
            "  )";
   }

   private GeneratedModStatement generateUpdateStatement
      (
         ModSpec modSpec
      )
   {
      String sql = makeUpdateSql(modSpec);

      List<String> targetFieldParams = getTargetFieldParamNames(modSpec);

      List<String> conditionParams = getConditionParamNames(modSpec);

      return new GeneratedModStatement(modSpec, sql, targetFieldParams, conditionParams);
   }

   private String makeUpdateSql
      (
         ModSpec modSpec
      )
   {
      RelId relId = identifyTable(modSpec.getTable(), modSpec.getStatementName(), "update table name");

      if ( modSpec.getTargetFields().isEmpty() )
         throw specError(modSpec.getStatementName(), "target fields",
                        "At least one field is required in an update modification command.");

      verifyReferencedTableFields(modSpec, relId);

      String fieldAssignments =
         modSpec.getTargetFields().stream()
         .map(f -> f.getField() + " = " + f.getValue())
         .collect(joining(",\n"));

      @Nullable String whereCond = getCondition(modSpec);

      return
         "update " + minimalRelIdentifier(relId) + applyOr(modSpec.getTableAlias(), a -> " " + a, "") + "\n" +
            "set\n" + indentLines(fieldAssignments, indentSpaces) +
            applyOr(whereCond, cond -> "\nwhere (\n" + indentLines(cond, indentSpaces) + "\n" + ")", "");
   }

   private GeneratedModStatement generateDeleteStatement
      (
         ModSpec modSpec
      )
   {
      if ( !modSpec.getTargetFields().isEmpty() )
         throw specError(modSpec.getStatementName(), "target fields",
                        "Fields are not allowed in a delete command.");

      String sql = makeDeleteSql(modSpec);

      List<String> conditionParams = getConditionParamNames(modSpec);

      return new GeneratedModStatement(modSpec, sql, emptyList(), conditionParams);
   }

   private String makeDeleteSql
      (
         ModSpec modSpec
      )
   {
      RelId relId = identifyTable(modSpec.getTable(), modSpec.getStatementName(), "delete table name");

      verifyReferencedTableFields(modSpec, relId);

      @Nullable String whereCond = getCondition(modSpec);

      return
         "delete from " + minimalRelIdentifier(relId) + applyOr(modSpec.getTableAlias(), a -> " " + a, "") +
         applyOr(whereCond, cond -> "\nwhere (\n" + indentLines(cond, indentSpaces) + "\n" + ")", "");
   }

   /// Return names for parameters used in the target fields of the passed mod statement specification.
   /// If params are of numbered type ("?" params), then the parameter names are merely descriptive and are
   /// used to determine source code member names which store the parameter numbers.
   private List<String> getTargetFieldParamNames(ModSpec modSpec)
   {
      List<String> res = new ArrayList<>();

      for ( TargetField targetField : modSpec.getTargetFields() )
      {
         if ( !targetField.getParamNames().isEmpty() )
         {
            validateExpressionValueParamNames(targetField, modSpec);
            res.addAll(targetField.getParamNames());
         }
         else if ( modSpec.getParametersType() == NAMED && simpleNamedParamValueRegex.matcher(targetField.getValue()).matches() )
         {
            res.add(targetField.getValue().substring(1));
         }
         else if ( modSpec.getParametersType() == NUMBERED && targetField.getValue().equals("?"))
         {
            res.add(lowerCamelCase(unDoubleQuote(targetField.getField())));
         }
         // It's not an error to arrive here, e.g. the field value may be an expression which depends on other field
         // field values or literals but not on any parameters.
      }

      return res;
   }

   private void validateExpressionValueParamNames
      (
         TargetField targetField,
         ModSpec modSpec
      )
   {
      // For named parameters, check that the declared parameters actually occur in the value expression string.
      if ( modSpec.getParametersType() == NAMED )
      {
         for ( String exprValParam : targetField.getParamNames() )
            if ( !targetField.getValue().contains(":" + exprValParam) )
               throw specError(modSpec.getStatementName(),
                  "field values list",
                  "Param \"" + exprValParam + "\" was not detected in value expresion for " +
                     "input field \"" + targetField.getField() + "\".");
      }
      else if ( modSpec.getParametersType() == NUMBERED )
      {
         if ( StringFuns.countOccurrences(targetField.getValue(), '?') < targetField.getParamNames().size() )
            throw specError(modSpec.getStatementName(),
               "field values list",
               "Not enough '?' parameters detected in value expresion vs specified param names for " +
                  "input field \"" + targetField.getField() + "\"."
            );
      }
   }

   private String getDefaultFieldConditionParamName(String fieldName)
   {
      return lowerCamelCase(unDoubleQuote(fieldName)) + "Cond";
   }

   private @Nullable String getCondition(ModSpec modSpec)
   {
      List<String> conds = new ArrayList<>();

      for ( FieldParamCondition fieldParamCond : modSpec.getFieldParamConditions() )
      {
         conds.add(
            sqlDialect.getFieldParamConditionSql(
               fieldParamCond,
               modSpec.getTableAlias(),
               modSpec.getParametersType(),
               this::getDefaultFieldConditionParamName
            )
         );
      }

      ifPresent(modSpec.getRecordCondition(), cond -> {
         @Nullable String tableAlias = modSpec.getTableAlias();
         if ( tableAlias != null )
         {
            String tableAliasVar = valueOr(cond.getWithTableAliasAs(), DEFAULT_TABLE_ALIAS_VAR);
            conds.add("(" + cond.getSql().replace(tableAliasVar, tableAlias) + ")");
         }
         else
            conds.add("(" + cond.getSql() + ")");
      });

      return conds.isEmpty() ? null : String.join("\nand\n", conds);
   }

   private List<String> getConditionParamNames(ModSpec modSpec)
   {
      // Since the parameters may be numbered, the ordering of params here must
      // match their occurrence in the sql generated in getCondition().
      List<String> res =
          modSpec.getFieldParamConditions().stream()
          .map(eq -> valueOr(eq.getParamName(), getDefaultFieldConditionParamName(eq.getField())))
          .collect(toList());

      ifPresent(modSpec.getRecordCondition(), cond ->
          res.addAll(cond.getParamNames())
      );

      return res;
   }

   /// Return a possibly qualified identifier for the given table, omitting the schema
   /// qualifier if it has a schema for which it's specified to use unqualified names.
   private String minimalRelIdentifier(RelId relId)
   {
      @Nullable String schema = relId.getSchema();
      if ( schema == null || unqualifiedNamesSchemas.contains(dbmd.normalizeName(schema)) )
         return relId.getName();
      else
         return relId.getIdString();
   }

   private void verifyReferencedTableFields
      (
         ModSpec modSpec,
         RelId relId
      )
      throws StatementSpecificationException
   {
      RelMetadata relMetadata = valueOrThrow(dbmd.getRelationMetadata(relId), () ->
         new RuntimeException("Table " + relId.toString() + " not found.")
      );

      String stmtName = modSpec.getStatementName();

      List<String> targetFields = modSpec.getTargetFields().stream().map(TargetField::getField).collect(toList());
      verifyTableFieldsExist(targetFields, relMetadata, dbmd, statementsSource, stmtName, "target fields");

      List<String> whereCondFields = modSpec.getFieldParamConditions().stream().map(FieldParamCondition::getField).collect(toList());
      verifyTableFieldsExist(whereCondFields, relMetadata, dbmd, statementsSource, stmtName, "where condition");
   }

   private StatementSpecificationException specError
      (
         String statementName,
         String statementPart,
         String problem
      )
   {
      return new
         StatementSpecificationException(
            statementsSource,
            statementName,
            statementPart,
            problem
         );
   }

   private RelId identifyTable
      (
         String table,
         String statementName,
         String statementPart
      )
      throws StatementSpecificationException
   {
      return identifySpecificationTable(table, defaultSchema, dbmd, statementsSource, statementName, statementPart);
   }
}

