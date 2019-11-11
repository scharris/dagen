package org.sqljson.specs.mod_stmts;

import java.util.List;
import java.util.Optional;
import static java.util.Optional.empty;
import static java.util.Collections.emptyList;

import static org.sqljson.specs.mod_stmts.ParametersType.NAMED;


public class ModSpec
{
   private String statementName;
   private ModType command;
   private String tableName; // possibly qualified
   private Optional<String> tableAlias = empty();
   private ParametersType parametersType = NAMED;
   private boolean generateSourceCode = true; // sql resource name and param info
   private List<TableInputField> inputFields = emptyList();
   private List<FieldParamEquality> fieldParamEqualities = emptyList();
   private Optional<String> otherCondition = empty(); // augments field param equalities

   private ModSpec() {}

   public ModSpec
   (
      String statementName,
      ModType command,
      String tableName,
      Optional<String> tableAlias,
      ParametersType parametersType,
      boolean generateSourceCode,
      List<TableInputField> inputFields,
      List<FieldParamEquality> fieldParamEqualities,
      Optional<String> otherCondition
   )
   {
      this.statementName = statementName;
      this.command = command;
      this.tableName = tableName;
      this.tableAlias = tableAlias;
      this.parametersType = parametersType;
      this.generateSourceCode = generateSourceCode;
      this.inputFields = inputFields;
      this.fieldParamEqualities = fieldParamEqualities;
      this.otherCondition = otherCondition;
   }

   public String getStatementName() { return statementName; }

   public ModType getCommand() { return command; }

   public String getTableName() { return tableName; }

   public Optional<String> getTableAlias() { return tableAlias; }

   public ParametersType getParametersType() { return parametersType; }

   public boolean getGenerateSourceCode() { return generateSourceCode; }

   public List<TableInputField> getInputFields() { return inputFields; }

   public List<FieldParamEquality> getFieldParamEqualities() { return fieldParamEqualities; }

   public Optional<String> getOtherCondition() { return otherCondition; }
}
