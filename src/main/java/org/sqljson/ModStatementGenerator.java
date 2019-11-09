package org.sqljson;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.*;

import org.sqljson.dbmd.DatabaseMetadata;
import org.sqljson.dbmd.Field;
import org.sqljson.dbmd.RelId;
import org.sqljson.dbmd.RelMetadata;
import org.sqljson.specs.mod_stmts.ParametersType;
import org.sqljson.util.AppUtils;
import org.sqljson.specs.mod_stmts.ModSpec;
import org.sqljson.specs.mod_stmts.TableInputField;
import static org.sqljson.specs.mod_stmts.ParametersType.NAMED;
import static org.sqljson.util.StringFuns.*;


public class ModStatementGenerator
{
   private final DatabaseMetadata dbmd;
   private final Optional<String> defaultSchema;
   private final Set<String> unqualifiedNamesSchemas;
   private final int indentSpaces;
   private static final Pattern simpleParamNameRegex = Pattern.compile(":[a-zA-Z0-9_]+");

   public ModStatementGenerator
   (
      DatabaseMetadata dbmd,
      Optional<String> defaultSchema,
      Set<String> unqualifiedNamesSchemas
   )
   {
      this.dbmd = dbmd;
      this.defaultSchema = defaultSchema;
      this.unqualifiedNamesSchemas = unqualifiedNamesSchemas.stream().map(dbmd::normalizeName).collect(toSet());
      this.indentSpaces = 2;
   }

   public List<GeneratedModStatement> generateModStatements(List<ModSpec> modSpecs)
   {
      return modSpecs.stream().map(this::generateModStatement).collect(toList());
   }

   private GeneratedModStatement generateModStatement(ModSpec mod)
   {
      validateModFieldValues(mod);

      switch ( mod.getCommand() )
      {
         case INSERT: return generateInsertStatement(mod);
         case UPDATE: return generateUpdateStatement(mod);
         case DELETE: return generateDeleteStatement(mod);
         default: throw new RuntimeException("Unexpected mod command " + mod.getCommand());
      }
   }

   private void validateModFieldValues(ModSpec modSpec)
   {
      boolean namedParams = modSpec.getParametersType() == NAMED;

      for ( TableInputField inputField : modSpec.getInputFields() )
      {
         String value = getFieldValue(inputField, modSpec);

         boolean notParenthesized = !value.startsWith("(") || !value.endsWith(")");

         if ( namedParams )
         {
            System.out.println("Checking value: " + value);
            if ( notParenthesized && !simpleParamNameRegex.matcher(value).matches() )
               throw new RuntimeException(
                  "Invalid parameter value for field \"" + inputField.getFieldName() + "\" " +
                  "of statement \"" + modSpec.getStatementName() + "\". General expressions must be parenthesized."
               );
         }
         else // numbered params
         {
            if ( notParenthesized && !value.equals("?") )
                  throw new RuntimeException(
                     "Numbered parameter \"" + inputField.getFieldName() + "\" value must be either \"?\" " +
                        "(the default) or else a parenthesized expression."
                  );
         }
      }
   }

   private GeneratedModStatement generateInsertStatement(ModSpec modSpec)
   {
      if ( modSpec.getTableAlias().isPresent() )
         AppUtils.throwError("A table alias is not allowed in an INSERT command.");
      if ( modSpec.getFilter().isPresent() )
         AppUtils.throwError("A filter is not allowed in an INSERT command.");

      String sql = makeInsertSql(modSpec);

      List<String> inputFieldParams = getInputFieldParamNames(modSpec);

      return new GeneratedModStatement(modSpec, sql, inputFieldParams);
   }

   private String makeInsertSql(ModSpec modSpec)
   {
      RelId relId = dbmd.identifyTable(modSpec.getTableName(), defaultSchema);
      RelMetadata tmd = getTableMetadata(relId);

      verifyFieldsExist(modSpec.getInputFields(), tmd);

      String fieldNames =
         modSpec.getInputFields().stream()
         .map(TableInputField::getFieldName)
         .collect(joining(","));

      String fieldValues =
         modSpec.getInputFields().stream()
         .map(f -> getFieldValue(f, modSpec))
         .collect(joining(",\n"));

      return
         "insert into " + minimalRelIdentifier(tmd.getRelationId()) + "\n" +
            "  (" + fieldNames + ")\n" +
            "values(\n" +
               indentLines(fieldValues, indentSpaces) + "\n" +
            ")" +
            modSpec.getFilter().map(cond -> "\nwhere " + cond).orElse("");
   }

   private GeneratedModStatement generateUpdateStatement(ModSpec modSpec)
   {
      String sql = makeUpdateSql(modSpec);

      List<String> inputFieldParams = getInputFieldParamNames(modSpec);

      return new GeneratedModStatement(modSpec, sql, inputFieldParams);
   }

   private String makeUpdateSql(ModSpec modSpec)
   {
      RelId relId = dbmd.identifyTable(modSpec.getTableName(), defaultSchema);
      RelMetadata tmd = getTableMetadata(relId);

      verifyFieldsExist(modSpec.getInputFields(), tmd);

      String fieldAssignments =
         modSpec.getInputFields().stream()
         .map(f -> f.getFieldName() + " = " + getFieldValue(f, modSpec))
         .collect(joining(",\n"));

      if ( modSpec.getInputFields().isEmpty() )
         throw new RuntimeException("At least one field is required in an update modification command.");

      return
         "update " + minimalRelIdentifier(tmd.getRelationId()) +
            modSpec.getTableAlias().map(a -> " " + a).orElse("") + "\n" +
            "set\n" +
            indentLines(fieldAssignments, indentSpaces) +
            modSpec.getFilter().map(cond -> "\nwhere " + cond).orElse("");
   }

   private GeneratedModStatement generateDeleteStatement(ModSpec modSpec)
   {
      if ( !modSpec.getInputFields().isEmpty() )
         AppUtils.throwError("Fields are not allowed in a delete command.");

      String sql = makeDeleteSql(modSpec.getTableName(), modSpec.getTableAlias(), modSpec.getFilter());

      return new GeneratedModStatement(modSpec, sql, emptyList());
   }

   private String makeDeleteSql
   (
      String tableName,
      Optional<String> tableAlias,
      Optional<String> whereCond
   )
   {
      RelId relId = dbmd.identifyTable(tableName, defaultSchema);
      RelMetadata tmd = getTableMetadata(relId);

      return
         "delete from " + minimalRelIdentifier(tmd.getRelationId()) + tableAlias.map(a -> " " + a).orElse("") +
         whereCond.map(cond -> "\nwhere " + cond).orElse("");
   }

   private String getFieldValue
   (
      TableInputField f,
      ModSpec modSpec
   )
   {
      return f.getValue().orElseGet(() ->
         getDefaultParamValueExpression(f, modSpec)
      );
   }

   private String getDefaultParamName(String inputFieldName)
   {
      return lowerCamelCase(unDoubleQuote(inputFieldName));
   }

   private String getDefaultParamValueExpression(TableInputField inputField, ModSpec modSpec)
   {
      switch ( modSpec.getParametersType() )
      {
         case NAMED: return  ":" + getDefaultParamName(inputField.getFieldName());
         case NUMBERED: return "?";
         default: throw new RuntimeException("Unexpected parameter name default enumeration value.");
      }
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
         {
            switch ( modSpec.getParametersType() )
            {
               case NAMED:
               {
                  String paramValueExpr = getFieldValue(inputField, modSpec);

                  if ( !paramValueExpr.startsWith(":") )
                     throw new RuntimeException(
                        "Failed to determine parameter name for field \"" + inputField.getFieldName() + "\" " +
                        "in statement specification \"" + modSpec.getStatementName() + "\" (expected leading  ':')."
                     );

                  res.add(paramValueExpr.substring(1));
                  break;
               }
               case NUMBERED:
                  res.add(getDefaultParamName(inputField.getFieldName()));
                  break;
               default:
                  throw new RuntimeException("Unrecognized parameters type " + modSpec.getParametersType());
            }
         }
         else // Input field spec has custom expression value (parenthesized), it must list any involved param names.
         {
            String valueExpression = inputField.getValue().get(); // Value must be present, else it would have defaulted
                                                                  // to just a simple param value.
            // Check that the declared parameters actually occur in the value expression string.
            if ( modSpec.getParametersType() == NAMED )
            {
               for ( String exprValParam : inputField.getExpressionValueParamNames() )
                  if ( !valueExpression.contains(":" + exprValParam) )
                     throw new RuntimeException(
                        "Param \"" + exprValParam + "\" not detected in value expresion for input field " +
                           "\"" + inputField.getFieldName() + "\" of statement \"" + modSpec.getStatementName() + "\"."
                     );
            }

            res.addAll(inputField.getExpressionValueParamNames());
         }
      }

      return res;
   }

   private void verifyFieldsExist
   (
      List<TableInputField> tableInputFields,
      RelMetadata tableMetadata
   )
   {
      Set<String> dbmdTableFields = tableMetadata.getFields().stream().map(Field::getName).collect(toSet());

      List<String> missingTableInputFields =
         tableInputFields.stream()
            .map(TableInputField::getFieldName)
            .filter(fieldName -> !dbmdTableFields.contains(dbmd.normalizeName(fieldName)))
            .collect(toList());

      if ( !missingTableInputFields.isEmpty() )
         throw new RuntimeException(
            "Field(s) not found in table " + tableMetadata.getRelationId() + ": " + missingTableInputFields + "."
         );
   }

   private RelMetadata getTableMetadata(RelId relId)
   {
      return dbmd.getRelationMetadata(relId).orElseThrow(() ->
         new RuntimeException("Table " + relId + " not found.")
      );
   }

   public String getDefaultInputFieldValue(String inputFieldName, ParametersType parametersType)
   {
      switch ( parametersType )
      {
         case NAMED: return  ":" + getDefaultParamName(inputFieldName);
         case NUMBERED: return "?";
         default: throw new RuntimeException("Unexpected parameter name default enumeration value.");
      }
   }

   /// Return a possibly qualified identifier for the given table, omitting the schema
   /// qualifier if it has a schema for which it's specified to use unqualified names.
   private String minimalRelIdentifier(RelId relId)
   {
      if ( !relId.getSchema().isPresent() ||
         unqualifiedNamesSchemas.contains(dbmd.normalizeName(relId.getSchema().get())) )
         return relId.getName();
      else
         return relId.getIdString();
   }

   private String commaJoinFieldNames(List<TableInputField> fields)
   {
      return fields.stream().map(TableInputField::getFieldName).collect(joining(","));
   }
}
