package io.sqljson.specs.mod_stmts;


public class ModSql
{
   private final String modName;
   private final String sql;

   public ModSql(String modName, String sql)
   {
      this.modName = modName;
      this.sql = sql;
   }

   public String getModName() { return modName; }

   public String getSql() { return sql; }
}
