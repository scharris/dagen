package org.sqljson.sql;

import org.checkerframework.checker.nullness.qual.Nullable;


public class ColumnMetadata
{
   private final String name;
   private final SelectClauseEntry.Source source;
   private final @Nullable String generatedFieldType;

   public ColumnMetadata
      (
         String name,
         SelectClauseEntry.Source source
      )
   {
      this(name, source, null);
   }

   public ColumnMetadata
      (
         String name,
         SelectClauseEntry.Source source,
         @Nullable String generatedFieldType
      )
   {
      this.name = name;
      this.source = source;
      this.generatedFieldType = generatedFieldType;
   }

   public String getName() { return name; }

   public SelectClauseEntry.Source getSource() { return source; }

   public @Nullable String getGeneratedFieldType() { return generatedFieldType; }
}

