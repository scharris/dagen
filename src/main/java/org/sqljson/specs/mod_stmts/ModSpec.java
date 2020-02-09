package org.sqljson.specs.mod_stmts;

import java.util.List;
import static java.util.Collections.emptyList;
import org.checkerframework.checker.nullness.qual.Nullable;

import org.sqljson.specs.FieldParamCondition;
import static org.sqljson.specs.mod_stmts.ParametersType.NAMED;


public class ModSpec
{
   private String statementName;
   private ModType command;
   private String table; // possibly qualified
   private @Nullable String tableAlias = null;
   private ParametersType parametersType = NAMED;
   private boolean generateSourceCode = true; // sql resource name and param info
   private List<TableInputField> inputFields = emptyList();
   private List<FieldParamCondition> fieldParamConditions = emptyList();
   private @Nullable String condition = null; // augments field param equalities

   private ModSpec()
   {
      this.statementName = "";
      this.command = ModType.DELETE;
      this.table = "";
   }

   public ModSpec
   (
      String statementName,
      ModType command,
      String table,
      @Nullable String tableAlias,
      ParametersType parametersType,
      boolean generateSourceCode,
      List<TableInputField> inputFields,
      List<FieldParamCondition> fieldParamConditions,
      @Nullable String condition
   )
   {
      this.statementName = statementName;
      this.command = command;
      this.table = table;
      this.tableAlias = tableAlias;
      this.parametersType = parametersType;
      this.generateSourceCode = generateSourceCode;
      this.inputFields = inputFields;
      this.fieldParamConditions = fieldParamConditions;
      this.condition = condition;
   }

   public String getStatementName() { return statementName; }

   public ModType getCommand() { return command; }

   public String getTable() { return table; }

   public @Nullable String getTableAlias() { return tableAlias; }

   public ParametersType getParametersType() { return parametersType; }

   public boolean getGenerateSourceCode() { return generateSourceCode; }

   public List<TableInputField> getInputFields() { return inputFields; }

   public List<FieldParamCondition> getFieldParamConditions() { return fieldParamConditions; }

   public @Nullable String getCondition() { return condition; }
}
