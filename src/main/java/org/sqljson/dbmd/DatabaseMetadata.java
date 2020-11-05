package org.sqljson.dbmd;

import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import static org.sqljson.dbmd.CaseSensitivity.INSENSITIVE_STORED_LOWER;
import static org.sqljson.dbmd.CaseSensitivity.INSENSITIVE_STORED_UPPER;
import static org.sqljson.common.util.Nullables.valueOr;

@JsonPropertyOrder({
   "schemaName", "dbmsName", "dbmsVersion", "dbmsMajorVersion",
   "dbmsMinorVersion", "caseSensitivity", "relationMetadatas", "foreignKeys"
})
public class DatabaseMetadata
{
   private @Nullable String schemaName;

   private List<RelMetadata> relationMetadatas;

   private List<ForeignKey> foreignKeys;

   private CaseSensitivity caseSensitivity;

   private String dbmsName;

   private String dbmsVersion;

   private int dbmsMajorVersion;

   private int dbmsMinorVersion;

   private static final Predicate<String> lc_ = Pattern.compile("^[a-z_]+$").asPredicate();
   private static final Predicate<String> uc_ = Pattern.compile("^[A-Z_]+$").asPredicate();

   // derived data
   // Access these only via methods of the same name, which make sure these fields are initialized.
   private @MonotonicNonNull Map<RelId, RelMetadata> relMDsByRelId;

   private @MonotonicNonNull Map<RelId, List<ForeignKey>> fksByParentRelId;

   private @MonotonicNonNull Map<RelId, List<ForeignKey>> fksByChildRelId;

   public DatabaseMetadata
      (
         @Nullable String schemaName,
         List<RelMetadata> relationMetadatas,
         List<ForeignKey> foreignKeys,
         CaseSensitivity caseSensitivity,
         String dbmsName,
         String dbmsVersion,
         int dbmsMajorVersion,
         int dbmsMinorVersion
      )
   {
      this.schemaName = schemaName;
      this.relationMetadatas = sortedMds(requireNonNull(relationMetadatas));
      this.foreignKeys = sortedFks(requireNonNull(foreignKeys));
      this.caseSensitivity = requireNonNull(caseSensitivity);
      this.dbmsName = requireNonNull(dbmsName);
      this.dbmsVersion = requireNonNull(dbmsVersion);
      this.dbmsMajorVersion = dbmsMajorVersion;
      this.dbmsMinorVersion = dbmsMinorVersion;
   }

   DatabaseMetadata()
   {
      relationMetadatas = emptyList();
      foreignKeys = emptyList();
      caseSensitivity = INSENSITIVE_STORED_LOWER;
      dbmsName = "";
      dbmsVersion = "";
   }

   public @Nullable String getSchemaName() { return schemaName; }

   public List<RelMetadata> getRelationMetadatas() { return relationMetadatas; }

   public List<ForeignKey> getForeignKeys() { return foreignKeys; }

   public CaseSensitivity getCaseSensitivity() { return caseSensitivity; }

   public String getDbmsName() { return dbmsName; }

   public String getDbmsVersion() { return dbmsVersion; }

   public int getDbmsMajorVersion() { return dbmsMajorVersion; }

   public int getDbmsMinorVersion() { return dbmsMinorVersion; }


   public @Nullable RelMetadata getRelationMetadata(RelId relId)
   {
      return relMDsByRelId().get(relId);
   }

   public @Nullable RelMetadata getRelationMetadata
      (
         @Nullable String schema,
         String relName
      )
   {
      return getRelationMetadata(makeRelId(schema, relName));
   }

   public List<String> getFieldNames
      (
         RelId relId,
         @Nullable String alias
      )
   {
      RelMetadata relMd = requireNonNull(getRelationMetadata(relId));

      return
         relMd.getFields().stream()
         .map(f -> dotQualify(alias, f.getName()))
         .collect(toList());
   }

   public List<String> getFieldNames(RelId relId)
   {
      return getFieldNames(relId, null);
   }

   public List<String> getFieldNames
      (
         @Nullable String schema,
         String relName
      )
   {
      return getFieldNames(makeRelId(schema, relName));
   }

   public List<String> getFieldNames
      (
         @Nullable String schema,
         String relName,
         @Nullable String alias
      )
   {
      return getFieldNames(makeRelId(schema, relName), alias);
   }

   public List<String> getPrimaryKeyFieldNames
      (
         RelId relId,
         @Nullable String alias
      )
   {
      RelMetadata relMd = requireNonNull(getRelationMetadata(relId));

      return relMd.getPrimaryKeyFieldNames(alias);
   }

   public List<String> getPrimaryKeyFieldNames(RelId relId)
   {
      return getPrimaryKeyFieldNames(relId, null);
   }

   public List<String> getPrimaryKeyFieldNames
      (
         @Nullable String schema,
         String relName
      )
   {
      return getPrimaryKeyFieldNames(makeRelId(schema, relName));
   }

   public List<String> getPrimaryKeyFieldNames
      (
         @Nullable String schema,
         String relName,
         @Nullable String alias
      )
   {
      return getPrimaryKeyFieldNames(makeRelId(schema, relName), alias);
   }

   public List<ForeignKey> getForeignKeysToParentsFrom(RelId relId)
   {
      return getForeignKeysFromTo(relId, null);
   }

   public List<ForeignKey> getForeignKeysToParentsFrom
      (
         @Nullable String schema,
         String relName
      )
   {
      return getForeignKeysFromTo(makeRelId(schema, relName), null);
   }

   public List<ForeignKey> getForeignKeysFromChildrenTo(RelId relId)
   {
      return getForeignKeysFromTo(null, relId);
   }

   public List<ForeignKey> getForeignKeysFromChildrenTo
      (
         @Nullable String schema,
         String relName
      )
   {
      return getForeignKeysFromTo(null, makeRelId(schema, relName));
   }

   public List<ForeignKey> getForeignKeysFromTo
      (
         String fromSchema,
         String fromRelName,
         String toSchema,
         String toRelName
      )
   {
      RelId fromRelId = makeRelId(fromSchema, fromRelName);
      RelId toRelId = makeRelId(toSchema, toRelName);

      return getForeignKeysFromTo(fromRelId, toRelId);
   }

   public List<ForeignKey> getForeignKeysFromTo
      (
         @Nullable RelId childRelId,
         @Nullable RelId parentRelId,
         ForeignKeyScope fkScope
      )
   {
      List<ForeignKey> res = new ArrayList<>();

      if ( childRelId == null && parentRelId == null )
         res.addAll(foreignKeys);
      else if ( childRelId != null && parentRelId != null )
      {
         res.addAll(fksByChildRelId(childRelId));
         res.retainAll(fksByParentRelId(parentRelId));
      }
      else
         res.addAll(childRelId != null ? fksByChildRelId(childRelId) : fksByParentRelId(requireNonNull(parentRelId)));

      if ( fkScope == ForeignKeyScope.REGISTERED_TABLES_ONLY )
      {
         return
            res.stream()
            .filter(fk ->
               getRelationMetadata(fk.getSourceRelationId()) != null &&
               getRelationMetadata(fk.getTargetRelationId()) != null
            )
            .collect(toList());
      }
      else
         return res;
   }

   public List<ForeignKey> getForeignKeysFromTo
      (
         @Nullable RelId childRelId,
         @Nullable RelId parentRelId
      )
   {
      return
         getForeignKeysFromTo(
            childRelId,
            parentRelId,
            ForeignKeyScope.REGISTERED_TABLES_ONLY
         );
   }

   /** Return a single foreign key between the passed tables, having the specified field names if specified,
    *  or null if not found. IllegalArgumentException is thrown if multiple foreign keys satisfy the requirements.
    */
   public @Nullable ForeignKey getForeignKeyFromTo
      (
         RelId fromRelId,
         RelId toRelId,
         @Nullable Set<String> fieldNames,
         ForeignKeyScope fkScope
      )
   {
      @Nullable ForeignKey soughtFk = null;
      @Nullable Set<String> normdFkFieldNames = fieldNames != null ? normalizeNames(fieldNames) : null;

      for ( ForeignKey fk : getForeignKeysFromTo(fromRelId, toRelId, fkScope) )
      {
         if ( normdFkFieldNames == null || fk.sourceFieldNamesSetEqualsNormalizedNamesSet(normdFkFieldNames) )
         {
            if ( soughtFk != null ) // already found an fk satisfying requirements?
               throw new IllegalArgumentException(
                  "Child table " + fromRelId + " has multiple foreign keys to parent table " + toRelId +
                  (fieldNames != null ?
                  " with the same specified source fields."
                  : " and no foreign key fields were specified to disambiguate.")
               );

            soughtFk = fk;
            // No breaking from the loop here, so case that multiple fk's satisfy requirements can be detected.
         }
      }

      return soughtFk;
   }

   /** Return the field names in the passed table involved in foreign keys (to parents). */
   public Set<String> getForeignKeyFieldNames
      (
         RelId relId,
         @Nullable String alias
      )
   {
      return
         getForeignKeysToParentsFrom(relId).stream()
         .flatMap(fk -> fk.getForeignKeyComponents().stream())
         .map(fkComp -> dotQualify(alias, fkComp.getForeignKeyFieldName()))
         .collect(toSet());
   }

   public Set<RelId> getMultiplyReferencingChildTablesForParent(RelId parentRelId)
   {
      Set<RelId> rels = new HashSet<>();
      Set<RelId> repeatedChildren = new HashSet<>();

      for ( ForeignKey fk : getForeignKeysFromChildrenTo(parentRelId) )
      {
         if ( !rels.add(fk.getSourceRelationId()) )
            repeatedChildren.add(fk.getSourceRelationId());
      }

      return repeatedChildren;
   }

   public Set<RelId> getMultiplyReferencedParentTablesForChild(RelId childRelId)
   {
      Set<RelId> rels = new HashSet<>();
      Set<RelId> repeatedParents = new HashSet<>();

      for ( ForeignKey fk : getForeignKeysToParentsFrom(childRelId) )
      {
         if ( !rels.add(fk.getTargetRelationId()) )
            repeatedParents.add(fk.getTargetRelationId());
      }

      return repeatedParents;
   }

   public @Nullable ForeignKey getForeignKeyHavingFieldSetAmong
      (
         Set<String> srcFieldNames,
         Collection<ForeignKey> fks
      )
   {
      Set<String> normdFieldNames = normalizeNames(srcFieldNames);

      return
         fks.stream()
         .filter(fk -> fk.sourceFieldNamesSetEqualsNormalizedNamesSet(normdFieldNames))
         .findAny()
         .orElse(null);
   }

   /////////////////////////////////////////////////////////
   // Sorting for deterministic output

   private static List<RelMetadata> sortedMds(List<RelMetadata> relMds)
   {
      List<RelMetadata> rmds = new ArrayList<>(relMds);

      rmds.sort(Comparator.comparing(rmd -> rmd.getRelationId().getIdString()));

      return unmodifiableList(rmds);
   }

   /**
    * Return a new copy of the input list, with its foreign keys sorted by source and target relation names and source and target field names.
    */
   private static List<ForeignKey> sortedFks(List<ForeignKey> foreignKeys)
   {
      List<ForeignKey> fks = new ArrayList<>(foreignKeys);

      fks.sort((fk1, fk2) -> {
         int srcRelComp = fk1.getSourceRelationId().getIdString().compareTo(fk2.getSourceRelationId().getIdString());
         if (srcRelComp != 0)
            return srcRelComp;

         int tgtRelComp = fk1.getTargetRelationId().getIdString().compareTo(fk2.getTargetRelationId().getIdString());
         if (tgtRelComp != 0)
            return tgtRelComp;

         int srcFieldsComp = compareStringListsLexicographically(fk1.getSourceFieldNames(), fk2.getSourceFieldNames());

         if (srcFieldsComp != 0)
            return srcFieldsComp;
         else
            return compareStringListsLexicographically(fk1.getTargetFieldNames(), fk2.getTargetFieldNames());
      });

      return unmodifiableList(fks);
   }

   private static int compareStringListsLexicographically
      (
         List<String> strs1,
         List<String> strs2
      )
   {
      int commonCount = Math.min(strs1.size(), strs2.size());

      for ( int i = 0; i < commonCount; ++i )
      {
         int comp = strs1.get(i).compareTo(strs2.get(i));
         if (comp != 0)
            return comp;
      }

      return Integer.compare(strs1.size(), strs2.size());
   }

   // Sorting for deterministic output
   /////////////////////////////////////////////////////////


   /////////////////////////////////////////////////////////
   // Derived data accessor methods

   private Map<RelId, RelMetadata> relMDsByRelId()
   {
      if ( relMDsByRelId == null )
         initDerivedData();

      return requireNonNull(relMDsByRelId);
   }

   private List<ForeignKey> fksByParentRelId(RelId relId)
   {
      if ( fksByParentRelId == null )
         initDerivedData();

      return valueOr(requireNonNull(fksByParentRelId).get(relId), emptyList());
   }

   private List<ForeignKey> fksByChildRelId(RelId relId)
   {
      if ( fksByChildRelId == null )
         initDerivedData();

      return valueOr(requireNonNull(fksByChildRelId).get(relId), emptyList());
   }

   private void initDerivedData()
   {
      relMDsByRelId = new HashMap<>();
      fksByParentRelId = new HashMap<>();
      fksByChildRelId = new HashMap<>();

      for ( RelMetadata relMd : relationMetadatas)
         relMDsByRelId.put(relMd.getRelationId(), relMd);

      for ( ForeignKey fk : foreignKeys )
      {
         RelId srcRelId = fk.getSourceRelationId();
         RelId tgtRelId = fk.getTargetRelationId();

         List<ForeignKey> fksFromChild = fksByChildRelId.computeIfAbsent(srcRelId, k -> new ArrayList<>());
         fksFromChild.add(fk);

         List<ForeignKey> fksToParent = fksByParentRelId.computeIfAbsent(tgtRelId, k -> new ArrayList<>());
         fksToParent.add(fk);
      }
   }

   // Derived data accessor methods
   /////////////////////////////////////////////////////////

   /// Quote a database identifier only if it would be interpreted differently if quoted.
   public String quoteIfNeeded(String id)
   {
      if ( id.startsWith("\"") && id.endsWith("\"") )
         return id;
      if ( id.startsWith("_") )
         return "\"" + id + "\"";
      if ( caseSensitivity == INSENSITIVE_STORED_LOWER && lc_.test(id) )
         return id;
      if ( caseSensitivity == INSENSITIVE_STORED_UPPER && uc_.test(id) )
         return id;
      return "\"" + id + "\"";
   }

   // Normalize a database object name.
   public String normalizeName(String id)
   {
      if ( id.startsWith("\"") && id.endsWith("\"") )
         return id;
      else if ( caseSensitivity == INSENSITIVE_STORED_LOWER )
         return id.toLowerCase();
      else if ( caseSensitivity == INSENSITIVE_STORED_UPPER )
         return id.toUpperCase();
      else
         return id;
   }

   private Set<String> normalizeNames(Set<String> names)
   {
      return names.stream().map(this::normalizeName).collect(toSet());
   }

   private static String dotQualify
      (
         @Nullable String alias,
         String name
      )
   {
      return alias != null ? alias + "." + name : name;
   }


   public RelId makeRelId
      (
         @Nullable String schema,
         String relName
      )
   {
      return new RelId(schema != null ? normalizeName(schema) : null, normalizeName(relName));
   }

   public RelId makeRelId(String possiblySchemaQualifiedRelName)
   {
      @Nullable String schema;
      String relName;

      int dotix = possiblySchemaQualifiedRelName.indexOf('.');

      if ( dotix == -1 )
      {
         schema = getSchemaName();
         relName = possiblySchemaQualifiedRelName;
      }
      else
      {
         schema = possiblySchemaQualifiedRelName.substring(0, dotix);
         relName = possiblySchemaQualifiedRelName.substring(dotix + 1);
      }

      return new RelId(schema != null ? normalizeName(schema) : null, normalizeName(relName));
   }

   /// Make a relation id from a given qualified or unqualified table identifier
   /// and an optional default schema for interpreting unqualified table names.
   /// The table is not verified to exist in the metadata.
   public RelId toRelId(String table, @Nullable String defaultSchema)
   {
      int dotIx = table.indexOf('.');

      if ( dotIx != -1 ) // already qualified, split it
         return new
            RelId(
               normalizeName(table.substring(dotIx+1)),
               normalizeName(table.substring(0, dotIx))
            );
      else // not qualified, qualify it if there is a default schema
         return new RelId(defaultSchema != null ? normalizeName(defaultSchema) : null, normalizeName(table));
   }
}

