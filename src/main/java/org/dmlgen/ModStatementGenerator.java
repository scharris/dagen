package org.dmlgen;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import static java.util.stream.Collectors.*;

import org.dmlgen.dbmd.DatabaseMetadata;
import org.dmlgen.dbmd.Field;
import org.dmlgen.dbmd.RelId;
import org.dmlgen.dbmd.RelMetadata;
import org.dmlgen.specs.mod_stmts.ModSpec;
import org.dmlgen.specs.mod_stmts.ModSql;
import org.dmlgen.specs.mod_stmts.TableInputField;
import static org.dmlgen.util.AppUtils.throwError;
import static org.dmlgen.util.StringFuns.indentLines;


public class ModStatementGenerator
{
   private final DatabaseMetadata dbmd;
   private final Optional<String> defaultSchema;
   private final Set<String> generateUnqualifiedNamesForSchemas;
   private final int indentSpaces;

   public ModStatementGenerator
   (
      DatabaseMetadata dbmd,
      Optional<String> defaultSchema,
      Set<String> generateUnqualifiedNamesForSchemas
   )
   {
      this.dbmd = dbmd;
      this.defaultSchema = defaultSchema;
      this.generateUnqualifiedNamesForSchemas =
         generateUnqualifiedNamesForSchemas.stream().map(dbmd::normalizeName).collect(toSet());
      this.indentSpaces = 2;
   }

   public List<ModSql> generateModSqls(List<ModSpec> modSpecs)
   {
      return modSpecs.stream().map(this::makeModSql).collect(toList());
   }

   private ModSql makeModSql(ModSpec mod)
   {
      switch ( mod.getCommand() )
      {
         case INSERT:
            if ( mod.getTableAlias().isPresent() )
               throwError("A table alias is not allowed in an INSERT command.");
            if ( mod.getFilter().isPresent() )
               throwError("A filter is not allowed in an INSERT command.");
            return new ModSql(
               mod.getModificationName(),
               makeInsertSql(
                  mod.getTableName(),
                  mod.getFilter(),
                  mod.getFields(),
                  mod.getDefaultParameterNameFunction()
               )
            );
         case UPDATE:
            return new ModSql(
               mod.getModificationName(),
               makeUpdateSql(
                  mod.getTableName(),
                  mod.getTableAlias(),
                  mod.getFilter(),
                  mod.getFields(),
                  mod.getDefaultParameterNameFunction()
               )
            );
         case DELETE:
            if ( !mod.getFields().isEmpty() )
               throwError("Fields are not allowed in a delete command.");
            return new ModSql(
               mod.getModificationName(),
               makeDeleteSql(
                  mod.getTableName(),
                  mod.getTableAlias(),
                  mod.getFilter()
               )
            );
         default:
            throw new RuntimeException("Unexpected mod command " + mod.getCommand());
      }
   }

   private String makeInsertSql
   (
      String tableName,
      Optional<String> whereCond,
      List<TableInputField> fields,
      Function<String,String> defaultParamNameFn
   )
   {
      RelId relId = dbmd.identifyTable(tableName, defaultSchema);
      RelMetadata tmd = getTableMetadata(relId);

      verifyFieldsExist(fields, tmd);

      String fieldNames = fields.stream().map(TableInputField::getFieldName).collect(joining(","));

      String fieldValues =
         fields.stream()
         .map(f -> getFieldValue(f, defaultParamNameFn))
         .collect(joining(",\n"));

      return
         "insert into " + minimalRelIdentifier(tmd.getRelationId()) + "\n" +
            "  (" + fieldNames + ")\n" +
            "values(\n" +
               indentLines(fieldValues, indentSpaces) + "\n" +
            ")" +
            whereCond.map(cond -> "\nwhere " + cond).orElse("");
   }

   private String makeUpdateSql
   (
      String tableName,
      Optional<String> tableAlias,
      Optional<String> whereCond,
      List<TableInputField> fields,
      Function<String,String> defaultParamNameFn
   )
   {
      RelId relId = dbmd.identifyTable(tableName, defaultSchema);
      RelMetadata tmd = getTableMetadata(relId);

      verifyFieldsExist(fields, tmd);

      String fieldAssignments =
         fields.stream()
         .map(f -> f.getFieldName() + " = " + getFieldValue(f, defaultParamNameFn))
         .collect(joining(",\n"));

      if ( fields.isEmpty() )
         throw new RuntimeException("At least one field is required in an update modification command.");

      return
         "update " + minimalRelIdentifier(tmd.getRelationId()) + tableAlias.map(a -> " " + a).orElse("") + "\n" +
            "set\n" +
            indentLines(fieldAssignments, indentSpaces) +
            whereCond.map(cond -> "\nwhere " + cond).orElse("");
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

   private String getFieldValue
   (
      TableInputField f,
      Function<String,String> defaultParamNameFn
   )
   {
      return f.getValue().orElseGet(() -> defaultParamNameFn.apply(f.getFieldName()));
   }

   private String commaJoinFieldNames(List<TableInputField> fields)
   {
      return fields.stream().map(TableInputField::getFieldName).collect(joining(","));
   }

   private RelMetadata getTableMetadata(RelId relId)
   {
      return dbmd.getRelationMetadata(relId).orElseThrow(() ->
         new RuntimeException("Table " + relId + " not found.")
      );
   }

   /// Return a possibly qualified identifier for the given table, omitting the schema
   /// qualifier if it has a schema for which it's specified to use unqualified names.
   private String minimalRelIdentifier(RelId relId)
   {
      if ( !relId.getSchema().isPresent() ||
         generateUnqualifiedNamesForSchemas.contains(dbmd.normalizeName(relId.getSchema().get())) )
         return relId.getName();
      else
         return relId.getIdString();
   }
}
