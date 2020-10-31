package org.sqljson.mod_stmts;

import java.util.ArrayList;
import java.util.List;
import static java.util.Collections.unmodifiableList;

import org.sqljson.mod_stmts.specs.ModSpec;
import static org.sqljson.mod_stmts.specs.ParametersType.NAMED;


public class GeneratedModStatement
{
   private final ModSpec modSpec;
   private final String sql;
   private final List<String> targetFieldParamNames;
   private final List<String> conditionParamNames;

   public GeneratedModStatement
      (
         ModSpec modSpec,
         String sql,
         List<String> targetFieldParamNames,
         List<String> conditionParamNames
      )
   {
      this.modSpec = modSpec;
      this.sql = sql;
      this.targetFieldParamNames = unmodifiableList(new ArrayList<>(targetFieldParamNames));
      this.conditionParamNames = unmodifiableList(new ArrayList<>(conditionParamNames));
   }

   public ModSpec getModSpec() { return modSpec; }

   public String getSql() { return sql; }


   public List<String> getTargetFieldParamNames() { return targetFieldParamNames; }

   public List<String> getConditionParamNames() { return conditionParamNames; }

   public boolean getGenerateSource() { return modSpec.getGenerateSourceCode(); }

   public String getStatementName() { return modSpec.getStatementName(); }

   public boolean hasNamedParameters() { return modSpec.getParametersType() == NAMED; }

   public List<String> getAllParameterNames()
   {
      List<String> paramNames = new ArrayList<>();
      paramNames.addAll(targetFieldParamNames);
      paramNames.addAll(conditionParamNames);
      return paramNames;
   }
}

