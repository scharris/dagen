package org.sqljson.mod_stmts.specs;

import java.util.ArrayList;
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;

import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;


/// Represents a group of SQL insert, update, and delete statements.
public final class ModGroupSpec
{
   private @Nullable String defaultSchema = null;
   private List<String> generateUnqualifiedNamesForSchemas = emptyList();
   private List<ModSpec> modificationStatementSpecs = emptyList();

   private ModGroupSpec() {}

   public ModGroupSpec
      (
         @Nullable String defaultSchema,
         List<String> generateUnqualifiedNamesForSchemas,
         List<ModSpec> modificationStatementSpecs
      )
   {
      this.defaultSchema = defaultSchema;
      this.generateUnqualifiedNamesForSchemas = generateUnqualifiedNamesForSchemas;
      this.modificationStatementSpecs = unmodifiableList(new ArrayList<>(modificationStatementSpecs));
   }

   public @Nullable String getDefaultSchema() { return defaultSchema; }

   public List<String> getGenerateUnqualifiedNamesForSchemas() { return generateUnqualifiedNamesForSchemas; }

   public List<ModSpec> getModificationStatementSpecs() { return modificationStatementSpecs; }
}

