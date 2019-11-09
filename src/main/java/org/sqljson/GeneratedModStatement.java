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

   public GeneratedModStatement
   (
      ModSpec modSpec,
      String sql,
      List<String> inputFieldParamNames
   )
   {
      this.modSpec = modSpec;
      this.sql = sql;
      this.inputFieldParamNames = unmodifiableList(new ArrayList<>(inputFieldParamNames));
   }

   public ModSpec getModSpec() { return modSpec; }

   public String getSql() { return sql; }


   public List<String> getInputFieldParamNames() { return inputFieldParamNames; }

   public boolean getGenerateSource() { return modSpec.getGenerateSourceCode(); }

   public String getStatementName() { return modSpec.getStatementName(); }

   public boolean hasNamedParameters() { return modSpec.getParametersType() == NAMED; }
}
