package gov.fda.nctr.sqljsonquery.sql;

import java.util.List;
import java.util.Set;
import java.util.ArrayList;
import java.util.HashSet;
import static java.util.Collections.*;

import gov.fda.nctr.util.Pair;
import static gov.fda.nctr.util.StringFuns.*;


public class SqlQueryParts
{
   private final List<Pair<String,String>> selectEntries;
   private final List<String> fromEntries;
   private final List<String> whereEntries;
   private final Set<String> aliasesInScope;

   public SqlQueryParts()
   {
      this.selectEntries = new ArrayList<>();
      this.fromEntries = new ArrayList<>();
      this.whereEntries = new ArrayList<>();
      this.aliasesInScope = new HashSet<>();
   }

   public SqlQueryParts
   (
      List<Pair<String,String>> selectEntries,
      List<String> fromEntries,
      List<String> whereEntries,
      Set<String> aliasesInScope
   )
   {
      this.selectEntries = new ArrayList<>(selectEntries);
      this.fromEntries = new ArrayList<>(fromEntries);
      this.whereEntries = new ArrayList<>(whereEntries);
      this.aliasesInScope = new HashSet<>(aliasesInScope);
   }

   public void addSelectClauseEntry(String expr, String name) { selectEntries.add(Pair.make(expr, name)); }
   public void addSelectClauseEntries(List<Pair<String,String>> entries) { selectEntries.addAll(entries); }
   public void addFromClauseEntry(String entry) { fromEntries.add(entry); }
   public void addFromClauseEntries(List<String> entries) { fromEntries.addAll(entries); }
   public void addWhereClauseEntry(String entry) { whereEntries.add(entry); }
   public void addAliasToScope(String alias) { aliasesInScope.add(alias); }
   public void addAliasesToScope(Set<String> aliases) { aliasesInScope.addAll(aliases); }

   public List<Pair<String, String>> getSelectClauseEntries() { return unmodifiableList(selectEntries); }
   public List<String> getFromClauseEntries() { return unmodifiableList(fromEntries); }
   public List<String> getWhereClauseEntries() { return unmodifiableList(whereEntries); }
   public Set<String> getAliasesInScope() { return unmodifiableSet(aliasesInScope); }

   public String makeNewAliasFor(String dbObjectName)
   {
      String alias = makeNameNotInSet(lowercaseInitials(dbObjectName, "_"), aliasesInScope);
      aliasesInScope.add(alias);
      return alias;
   }

}
