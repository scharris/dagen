package org.sqljson;

import java.util.ArrayList;
import java.util.List;
import static java.util.Collections.unmodifiableList;

import org.sqljson.specs.mod_stmts.ModSpec;
import static org.sqljson.specs.mod_stmts.ParametersType.NAMED;


public class GeneratedModStatement
{
   private final ModSpec modSpec;
   private final String sql;
   private final List<String> inputFieldParamNames;
   private final List<String> conditionParamNames;

   public GeneratedModStatement
   (
      ModSpec modSpec,
      String sql,
      List<String> inputFieldParamNames,
      List<String> conditionParamNames
   )
   {
      this.modSpec = modSpec;
      this.sql = sql;
      this.inputFieldParamNames = unmodifiableList(new ArrayList<>(inputFieldParamNames));
      this.conditionParamNames = unmodifiableList(new ArrayList<>(conditionParamNames));
   }

   public ModSpec getModSpec() { return modSpec; }

   public String getSql() { return sql; }


   public List<String> getInputFieldParamNames() { return inputFieldParamNames; }

   public List<String> getConditionParamNames() { return conditionParamNames; }

   public boolean getGenerateSource() { return modSpec.getGenerateSourceCode(); }

   public String getStatementName() { return modSpec.getStatementName(); }

   public boolean hasNamedParameters() { return modSpec.getParametersType() == NAMED; }

   public List<String> getAllParameterNames()
   {
      List<String> paramNames = new ArrayList<>();
      paramNames.addAll(inputFieldParamNames);
      paramNames.addAll(conditionParamNames);
      return paramNames;
   }
}