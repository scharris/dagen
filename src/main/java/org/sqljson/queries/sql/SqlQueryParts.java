package org.sqljson.queries.sql;

import java.util.List;
import java.util.Set;
import java.util.ArrayList;
import java.util.HashSet;
import static java.util.Collections.*;
import static java.util.stream.Collectors.joining;

import org.checkerframework.checker.nullness.qual.Nullable;

import org.sqljson.common.util.StringFuns;
import static org.sqljson.common.util.Nullables.applyOr;
import static org.sqljson.common.util.StringFuns.indentLines;


public class SqlQueryParts
{
   private final List<SelectClauseEntry> selectEntries;
   private final List<String> fromEntries;
   private final List<String> whereEntries;
   private @Nullable String orderBy;
   private final Set<String> aliasesInScope;

   public SqlQueryParts()
   {
      this.selectEntries = new ArrayList<>();
      this.fromEntries = new ArrayList<>();
      this.whereEntries = new ArrayList<>();
      this.orderBy = null;
      this.aliasesInScope = new HashSet<>();
   }

   public SqlQueryParts
      (
         List<SelectClauseEntry> selectEntries,
         List<String> fromEntries,
         List<String> whereEntries,
         @Nullable String orderBy,
         Set<String> aliasesInScope
      )
   {
      this.selectEntries = new ArrayList<>(selectEntries);
      this.fromEntries = new ArrayList<>(fromEntries);
      this.whereEntries = new ArrayList<>(whereEntries);
      this.orderBy = orderBy;
      this.aliasesInScope = new HashSet<>(aliasesInScope);
   }

   public void addSelectClauseEntry
      (
         String expr,
         String name,
         SelectClauseEntry.Source src,
         @Nullable String comment
      )
   {
      selectEntries.add(new SelectClauseEntry(expr, name, src, comment));
   }

   public void addSelectClauseEntry
      (
         String expr,
         String name,
         SelectClauseEntry.Source src
      )
   {
      addSelectClauseEntry(expr, name, src, null);
   }

   public void addSelectClauseEntries(List<SelectClauseEntry> entries) { selectEntries.addAll(entries); }

   public void addFromClauseEntry(String entry) { fromEntries.add(entry); }
   public void addFromClauseEntries(List<String> entries) { fromEntries.addAll(entries); }

   public void addWhereClauseEntry(String entry) { whereEntries.add(entry); }
   public void addWhereClauseEntries(List<String> entries) { whereEntries.addAll(entries); }

   public void addAliasToScope(String alias) { aliasesInScope.add(alias); }
   public void addAliasesToScope(Set<String> aliases) { aliasesInScope.addAll(aliases); }

   public void setOrderBy(String orderByExpr) { orderBy = orderByExpr; }

   public void addParts(SqlQueryParts otherParts)
   {
      addSelectClauseEntries(otherParts.getSelectClauseEntries());
      addFromClauseEntries(otherParts.getFromClauseEntries());
      addWhereClauseEntries(otherParts.getWhereClauseEntries());
      addAliasesToScope(otherParts.getAliasesInScope());
   }

   public List<SelectClauseEntry> getSelectClauseEntries() { return unmodifiableList(selectEntries); }
   public List<String> getFromClauseEntries() { return unmodifiableList(fromEntries); }
   public List<String> getWhereClauseEntries() { return unmodifiableList(whereEntries); }
   public @Nullable String getOrderBy() { return orderBy; }
   public Set<String> getAliasesInScope() { return unmodifiableSet(aliasesInScope); }

   public String makeNewAliasFor(String dbObjectName)
   {
      String alias = StringFuns.makeNameNotInSet(StringFuns.lowercaseInitials(dbObjectName, "_"), aliasesInScope);
      aliasesInScope.add(alias);
      return alias;
   }

   public String toSql(int indentSpaces)
   {
      String selectEntriesStr =
         getSelectClauseEntries().stream()
         .map(SqlQueryParts::selectEntrySql)
         .collect(joining(",\n"));

      String fromEntriesStr = String.join("\n", getFromClauseEntries());

      String whereEntriesStr = String.join(" and\n", getWhereClauseEntries());

      return
         "select\n" +
            indentLines(selectEntriesStr, indentSpaces) + "\n" +
         "from\n" +
            indentLines(fromEntriesStr, indentSpaces) + "\n" +
         (getWhereClauseEntries().isEmpty() ? "":
            "where (\n" +
               indentLines(whereEntriesStr, indentSpaces) + "\n" +
            ")") +
         applyOr(getOrderBy(), orderBy -> "\norder by " + orderBy, "");
   }

   // Make sql string for a select clause entry.
   private static String selectEntrySql(SelectClauseEntry sce)
   {
      String exprNameSep = sce.getName().startsWith("\"") ? " " : " as ";
      @Nullable String comment = sce.getComment();
      return
         (comment != null ? comment + "\n" : "") +
         sce.getValueExpression() + exprNameSep + sce.getName();
   }
}

