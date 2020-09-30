package org.sqljson.sql;

import org.checkerframework.checker.nullness.qual.Nullable;


public class ColumnMetadata
{
   private final String name;
   private final @Nullable String generatedFieldType;

   public ColumnMetadata
      (
         String name
      )
   {
      this(name, null);
   }

   public ColumnMetadata
      (
         String name,
         @Nullable String generatedFieldType
      )
   {
      this.name = name;
      this.generatedFieldType = generatedFieldType;
   }

   public String getName() { return name; }

   public @Nullable String getGeneratedFieldType() { return generatedFieldType; }
}

