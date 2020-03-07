package org.sqljson;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.*;

import org.checkerframework.checker.nullness.qual.Nullable;

import org.sqljson.dbmd.DatabaseMetadata;
import org.sqljson.dbmd.RelId;
import org.sqljson.dbmd.RelMetadata;
import org.sqljson.specs.FieldParamCondition;
import org.sqljson.sql.dialect.SqlDialect;
import org.sqljson.util.AppUtils;
import org.sqljson.specs.mod_stmts.ModSpec;
import org.sqljson.specs.mod_stmts.TableInputField;
import static org.sqljson.specs.mod_stmts.ParametersType.NAMED;
import static org.sqljson.util.DatabaseUtils.verifyTableFieldsExist;
import static org.sqljson.util.Nullables.*;
import static org.sqljson.util.StringFuns.*;


public class ModStatementGenerator
{
   private final DatabaseMetadata dbmd;
   private final SqlDialect sqlDialect;
   private final @Nullable String defaultSchema;
   private final Set<String> unqualifiedNamesSchemas;
   private final int indentSpaces;
   private static final Pattern simpleParamNameRegex = Pattern.compile(":[a-zA-Z0-9_]+");

   public ModStatementGenerator
   (
      DatabaseMetadata dbmd,
      @Nullable String defaultSchema,
      Set<String> unqualifiedNamesSchemas
   )
   {
      this.dbmd = dbmd;
      this.indentSpaces = 2;
      this.sqlDialect = SqlDialect.fromDatabaseMetadata(this.dbmd, this.indentSpaces);
      this.defaultSchema = defaultSchema;
      this.unqualifiedNamesSchemas = unqualifiedNamesSchemas.stream().map(dbmd::normalizeName).collect(toSet());
   }

   public List<GeneratedModStatement> generateModStatements(List<ModSpec> modSpecs)
   {
      return modSpecs.stream().map(this::generateModStatement).collect(toList());
   }

   private GeneratedModStatement generateModStatement(ModSpec mod)
   {
      validateInputFieldValues(mod);

      switch ( mod.getCommand() )
      {
         case INSERT: return generateInsertStatement(mod);
         case UPDATE: return generateUpdateStatement(mod);
         case DELETE: return generateDeleteStatement(mod);
         default: throw new RuntimeException("Unexpected mod command " + mod.getCommand());
      }
   }

   private void validateInputFieldValues(ModSpec modSpec)
   {
      boolean namedParams = modSpec.getParametersType() == NAMED;

      for ( TableInputField inputField : modSpec.getInputFields() )
      {
         String value = getInputFieldValue(inputField, modSpec);

         boolean notParenthesized = !value.startsWith("(") || !value.endsWith(")");

         if ( namedParams )
         {
            if ( notParenthesized && !simpleParamNameRegex.matcher(value).matches() )
               throw new RuntimeException(
                  "Invalid parameter value for field \"" + inputField.getField() + "\" " +
                  "of statement \"" + modSpec.getStatementName() + "\". General expressions must be parenthesized."
               );
         }
         else // numbered params
         {
            if ( notParenthesized && !value.equals("?") )
                  throw new RuntimeException(
                     "Numbered parameter \"" + inputField.getField() + "\" value must be either \"?\" " +
                        "(the default) or else a parenthesized expression."
                  );
         }
      }
   }

   private GeneratedModStatement generateInsertStatement(ModSpec modSpec)
   {
      if ( modSpec.getTableAlias() != null )
         AppUtils.throwError("A table alias is not allowed in an INSERT command.");
      if ( !modSpec.getFieldParamConditions().isEmpty() || modSpec.getCondition() != null )
         AppUtils.throwError("Conditions are not allowed for INSERT commands.");

      String sql = makeInsertSql(modSpec);

      List<String> inputFieldParams = getInputFieldParamNames(modSpec);

      return new GeneratedModStatement(modSpec, sql, inputFieldParams, emptyList());
   }

   private String makeInsertSql(ModSpec modSpec)
   {
      RelId relId = dbmd.identifyTable(modSpec.getTable(), defaultSchema);

      verifyAllReferencedFieldsExist(modSpec, relId);

      String fieldNames =
         modSpec.getInputFields().stream()
         .map(TableInputField::getField)
         .collect(joining(",\n"));

      String fieldValues =
         modSpec.getInputFields().stream()
         .map(f -> getInputFieldValue(f, modSpec))
         .collect(joining(",\n"));

      return
         "insert into " + minimalRelIdentifier(relId) + "\n" +
            "  (\n" +
               indentLines(fieldNames, 2 + indentSpaces) + "\n" +
            "  )\n" +
            "values\n" +
            "  (\n" +
               indentLines(fieldValues, 2 + indentSpaces) + "\n" +
            "  )";
   }

   private GeneratedModStatement generateUpdateStatement(ModSpec modSpec)
   {
      String sql = makeUpdateSql(modSpec);

      List<String> inputFieldParams = getInputFieldParamNames(modSpec);

      List<String> conditionParams = getConditionParamNames(modSpec);

      return new GeneratedModStatement(modSpec, sql, inputFieldParams, conditionParams);
   }

   private String makeUpdateSql(ModSpec modSpec)
   {
      RelId relId = dbmd.identifyTable(modSpec.getTable(), defaultSchema);

      verifyAllReferencedFieldsExist(modSpec, relId);

      String fieldAssignments =
         modSpec.getInputFields().stream()
         .map(f -> f.getField() + " = " + getInputFieldValue(f, modSpec))
         .collect(joining(",\n"));

      if ( modSpec.getInputFields().isEmpty() )
         throw new RuntimeException("At least one field is required in an update modification command.");

      @Nullable String whereCond = getCondition(modSpec);

      return
         "update " + minimalRelIdentifier(relId) +
            applyOr(modSpec.getTableAlias(), a -> " " + a, "") + "\n" +
            "set\n" +
            indentLines(fieldAssignments, indentSpaces) +
            applyOr(whereCond, cond -> "\nwhere (\n" + indentLines(cond, indentSpaces) + "\n" + ")", "");
   }

   private GeneratedModStatement generateDeleteStatement(ModSpec modSpec)
   {
      if ( !modSpec.getInputFields().isEmpty() )
         AppUtils.throwError("Fields are not allowed in a delete command.");

      String sql = makeDeleteSql(modSpec);

      List<String> conditionParams = getConditionParamNames(modSpec);

      return new GeneratedModStatement(modSpec, sql, emptyList(), conditionParams);
   }

   private String makeDeleteSql(ModSpec modSpec)
   {
      RelId relId = dbmd.identifyTable(modSpec.getTable(), defaultSchema);

      verifyAllReferencedFieldsExist(modSpec, relId);

      @Nullable String whereCond = getCondition(modSpec);

      return
         "delete from " + minimalRelIdentifier(relId) + applyOr(modSpec.getTableAlias(), a -> " " + a, "") +
         applyOr(whereCond, cond -> "\nwhere (\n" + indentLines(cond, indentSpaces) + "\n" + ")", "");
   }

   private String getInputFieldValue
   (
      TableInputField f,
      ModSpec modSpec
   )
   {
      return valueOrGet(f.getValue(), () ->
         getDefaultParamValueExpression(f, modSpec)
      );
   }

   private String getDefaultParamValueExpression(TableInputField inputField, ModSpec modSpec)
   {
      switch ( modSpec.getParametersType() )
      {
         case NAMED: return  ":" + getDefaultInputFieldParamName(inputField.getField());
         case NUMBERED: return "?";
         default: throw new RuntimeException("Unexpected parameter name default enumeration value.");
      }
   }

   private String getDefaultInputFieldParamName(String inputFieldName)
   {
      return lowerCamelCase(unDoubleQuote(inputFieldName));
   }

   /// Return names for parameters used in the table input fields of the passed mod statement specification.
   /// If params are of numbered type ("?" params), then the parameter names are merely descriptive and are
   /// used to determine source code member names which store the parameter numbers.
   private List<String> getInputFieldParamNames(ModSpec modSpec)
   {
      List<String> res = new ArrayList<>();

      for ( TableInputField inputField : modSpec.getInputFields() )
      {
         if ( inputField.hasSimpleParamValue() )
            res.add(getSimpleInputFieldParamName(inputField, modSpec));
         else // Input field spec has custom expression value (parenthesized), it must list any involved param names.
         {
            validateExpressionInputField(inputField, modSpec);
            res.addAll(inputField.getExpressionValueParamNames());
         }
      }

      return res;
   }

   private String getSimpleInputFieldParamName(TableInputField inputField, ModSpec modSpec)
   {
      switch ( modSpec.getParametersType() )
      {
         case NAMED:
         {
            String paramValueExpr = getInputFieldValue(inputField, modSpec);

            if ( !paramValueExpr.startsWith(":") )
               throw new RuntimeException(
                  "Failed to determine parameter name for field \"" + inputField.getField() + "\" " +
                  "in statement specification \"" + modSpec.getStatementName() + "\" (expected leading  ':')."
               );

            return paramValueExpr.substring(1);
         }
         case NUMBERED:
            return getDefaultInputFieldParamName(inputField.getField());
         default:
            throw new RuntimeException("Unrecognized parameters type " + modSpec.getParametersType());
      }
   }

   private void validateExpressionInputField(TableInputField inputField, ModSpec modSpec)
   {
      String valueExpression = valueOrThrow(inputField.getValue(), () ->
          new RuntimeException("Programming error: input value should be present when not a simple param value.")
      );

      // Check that the declared parameters actually occur in the value expression string.
      if ( modSpec.getParametersType() == NAMED )
      {
         for ( String exprValParam : inputField.getExpressionValueParamNames() )
            if ( !valueExpression.contains(":" + exprValParam) )
               throw new RuntimeException(
                   "Param \"" + exprValParam + "\" not detected in value expresion for input field " +
                       "\"" + inputField.getField() + "\" of statement \"" + modSpec.getStatementName() + "\"."
               );
      }
   }

   private String getDefaultCondParamName(String inputFieldName)
   {
      return lowerCamelCase(unDoubleQuote(inputFieldName)) + "Cond";
   }


   private List<String> getConditionParamNames(ModSpec modSpec)
   {
      return
        modSpec.getFieldParamConditions().stream()
        .map(eq -> valueOr(eq.getParamName(), getDefaultCondParamName(eq.getField())))
        .collect(toList());
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
               this::getDefaultCondParamName
            )
         );
      }

      // Other condition goes last so it will not interfere with parameter numbering in case it introduces its own params.
      ifPresent(modSpec.getCondition(), cond ->
          conds.add("(" + cond + ")")
      );

      return conds.isEmpty() ? null : String.join("\nand\n", conds);
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

   private void verifyAllReferencedFieldsExist
   (
      ModSpec modSpec,
      RelId relId
   )
      throws DatabaseObjectsNotFoundException
   {
      RelMetadata relMetadata = valueOrThrow(dbmd.getRelationMetadata(relId), () ->
         new DatabaseObjectsNotFoundException("Table " + relId.toString() + " not found.")
      );

      List<String> inputFieldNames = modSpec.getInputFields().stream().map(TableInputField::getField).collect(toList());
      verifyTableFieldsExist(inputFieldNames, relMetadata, dbmd);

      List<String> whereCondFieldNames =
         modSpec.getFieldParamConditions().stream().map(FieldParamCondition::getField).collect(toList());
      verifyTableFieldsExist(whereCondFieldNames, relMetadata, dbmd);
   }

}
