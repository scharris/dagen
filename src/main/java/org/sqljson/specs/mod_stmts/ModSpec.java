package org.sqljson.specs.mod_stmts;

import java.util.List;
import static java.util.Collections.emptyList;
import org.checkerframework.checker.nullness.qual.Nullable;

import org.sqljson.specs.RecordCondition;
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
   private List<TargetField> targetFields = emptyList();
   private List<FieldParamCondition> fieldParamConditions = emptyList();
   private @Nullable RecordCondition recordCondition = null; // augments field param equalities

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
         List<TargetField> targetFields,
         List<FieldParamCondition> fieldParamConditions,
         @Nullable RecordCondition recordCondition
      )
   {
      this.statementName = statementName;
      this.command = command;
      this.table = table;
      this.tableAlias = tableAlias;
      this.parametersType = parametersType;
      this.generateSourceCode = generateSourceCode;
      this.targetFields = targetFields;
      this.fieldParamConditions = fieldParamConditions;
      this.recordCondition = recordCondition;
   }

   public String getStatementName() { return statementName; }

   public ModType getCommand() { return command; }

   public String getTable() { return table; }

   public @Nullable String getTableAlias() { return tableAlias; }

   public ParametersType getParametersType() { return parametersType; }

   public boolean getGenerateSourceCode() { return generateSourceCode; }

   public List<TargetField> getTargetFields() { return targetFields; }

   public List<FieldParamCondition> getFieldParamConditions() { return fieldParamConditions; }

   public @Nullable RecordCondition getRecordCondition() { return recordCondition; }
}

