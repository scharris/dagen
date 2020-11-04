package org.sqljson.queries.specs;

import java.util.List;

import org.checkerframework.checker.nullness.qual.Nullable;


public class RecordCondition
{
   private final String sql;
   private final @Nullable List<String> paramNames;
   private final @Nullable String withTableAliasAs; // table alias variable used in sql

   private RecordCondition()
   {
      this.sql =  "";
      this.paramNames = null;
      this.withTableAliasAs = null;
   }

   public RecordCondition
      (
         String sql,
         @Nullable List<String> paramNames,
         @Nullable String withTableAliasAs
      )
   {
      this.sql = sql;
      this.paramNames = paramNames;
      this.withTableAliasAs = withTableAliasAs;
   }

   public String getSql() { return sql; }

   public @Nullable List<String> getParamNames() { return paramNames; }

   public @Nullable String getWithTableAliasAs() { return withTableAliasAs; }
}

