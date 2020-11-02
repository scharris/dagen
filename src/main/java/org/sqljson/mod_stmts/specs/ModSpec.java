package org.sqljson.mod_stmts.specs;

import java.util.List;
import static java.util.Collections.emptyList;
import org.checkerframework.checker.nullness.qual.Nullable;

import org.sqljson.common.specs.RecordCondition;
import org.sqljson.common.specs.FieldParamCondition;
import com.fasterxml.jackson.annotation.JsonIgnore;

import static org.sqljson.mod_stmts.specs.ParametersType.NAMED;


public class ModSpec
{
   private final String statementName;
   private final ModType command;
   private final String table; // possibly qualified
   private final @Nullable String tableAlias;
   private final @Nullable List<TargetField> targetFields;
   private final @Nullable ParametersType parametersType;
   private final @Nullable Boolean generateSourceCode; // sql resource name and param info
   private final @Nullable List<FieldParamCondition> fieldParamConditions;
   private final @Nullable RecordCondition recordCondition; // augments field param equalities

   private ModSpec()
   {
      this.statementName = "";
      this.command = ModType.DELETE;
      this.table = "";
      this.tableAlias = null;
      this.targetFields = null;
      this.parametersType = null;
      this.generateSourceCode = true;
      this.fieldParamConditions = null;
      this.recordCondition = null;
   }

   public ModSpec
      (
         String statementName,
         ModType command,
         String table,
         @Nullable String tableAlias,
         @Nullable List<TargetField> targetFields,
         @Nullable ParametersType parametersType,
         @Nullable Boolean generateSourceCode,
         @Nullable List<FieldParamCondition> fieldParamConditions,
         @Nullable RecordCondition recordCondition
      )
   {
      this.statementName = statementName;
      this.command = command;
      this.table = table;
      this.tableAlias = tableAlias;
      this.targetFields = targetFields;
      this.parametersType = parametersType;
      this.generateSourceCode = generateSourceCode;
      this.fieldParamConditions = fieldParamConditions;
      this.recordCondition = recordCondition;
   }

   public String getStatementName() { return statementName; }

   public ModType getCommand() { return command; }

   public String getTable() { return table; }

   public @Nullable String getTableAlias() { return tableAlias; }

   public @Nullable ParametersType getParametersType() { return parametersType; }

   @JsonIgnore
   public ParametersType getParametersTypeOrDefault()
   {
      return parametersType != null ? parametersType : NAMED;
   }

   public @Nullable Boolean getGenerateSourceCode() { return generateSourceCode; }

   @JsonIgnore
   public boolean getGenerateSourceCodeOrDefault()
   {
      return generateSourceCode != null ? generateSourceCode: true;
   }

   public @Nullable List<TargetField> getTargetFields() { return targetFields; }

   @JsonIgnore
   public List<TargetField> getTargetFieldsList()
   {
      return targetFields != null ? targetFields: emptyList();
   }

   public @Nullable List<FieldParamCondition> getFieldParamConditions() { return fieldParamConditions; }

   @JsonIgnore
   public List<FieldParamCondition> getFieldParamConditionsList()
   {
      return fieldParamConditions != null ? fieldParamConditions: emptyList();
   }

   public @Nullable RecordCondition getRecordCondition() { return recordCondition; }
}

