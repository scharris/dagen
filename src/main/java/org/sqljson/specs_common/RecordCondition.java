package org.sqljson.specs_common;

import java.util.Collections;
import java.util.List;

import org.checkerframework.checker.nullness.qual.Nullable;


public class RecordCondition
{
   private String sql;
   private List<String> paramNames = Collections.emptyList();
   private @Nullable String withTableAliasAs = null; // table alias variable used in sql

   private RecordCondition()
   {
      this.sql =  "";
   }

   public RecordCondition
      (
         String sql,
         List<String> paramNames,
         @Nullable String withTableAliasAs
      )
   {
      this.sql = sql;
      this.paramNames = paramNames;
      this.withTableAliasAs = withTableAliasAs;
   }

   public String getSql() { return sql; }

   public List<String> getParamNames() { return paramNames; }

   public @Nullable String getWithTableAliasAs() { return withTableAliasAs; }
}

