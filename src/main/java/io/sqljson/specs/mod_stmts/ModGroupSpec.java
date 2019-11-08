package io.sqljson.specs.mod_stmts;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;
import static java.util.Optional.empty;


/// Represents a group of SQL insert, update, and delete statements.
public final class ModGroupSpec
{
   private Optional<String> defaultSchema = empty();
   private List<String> generateUnqualifiedNamesForSchemas = emptyList();
   private List<ModSpec> modSpecs = emptyList();

   private ModGroupSpec() {}

   public ModGroupSpec
   (
      Optional<String> defaultSchema,
      List<String> generateUnqualifiedNamesForSchemas,
      List<ModSpec> modSpecs
   )
   {
      this.defaultSchema = defaultSchema;
      this.generateUnqualifiedNamesForSchemas = generateUnqualifiedNamesForSchemas;
      this.modSpecs = unmodifiableList(new ArrayList<>(modSpecs));
   }

   public Optional<String> getDefaultSchema() { return defaultSchema; }

   public List<String> getGenerateUnqualifiedNamesForSchemas() { return generateUnqualifiedNamesForSchemas; }

   public List<ModSpec> getModSpecs() { return modSpecs; }
}


