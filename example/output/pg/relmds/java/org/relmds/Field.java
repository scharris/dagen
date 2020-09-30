// ---------------------------------------------------------------------------
// [ THIS SOURCE CODE WAS AUTO-GENERATED, ANY CHANGES MADE HERE MAY BE LOST. ]
// ---------------------------------------------------------------------------
package org.relmds;

import org.checkerframework.checker.nullness.qual.Nullable;



public class Field
{
   public String name;
   public int jdbcTypeCode;
   public String databaseType;
   public @Nullable Integer length;
   public @Nullable Integer precision;
   public @Nullable Integer fractionalDigits;
   public @Nullable Boolean nullable;
   public @Nullable Integer primaryKeyPartNumber;
   public Field
      (
         String name,
         int jdbcTypeCode,
         String databaseType,
         @Nullable Integer length,
         @Nullable Integer precision,
         @Nullable Integer fractionalDigits,
         @Nullable Boolean nullable,
         @Nullable Integer primaryKeyPartNumber
      )
   {
      this.name = name;
      this.jdbcTypeCode = jdbcTypeCode;
      this.databaseType = databaseType;
      this.length = length;
      this.precision = precision;
      this.fractionalDigits = fractionalDigits;
      this.nullable = nullable;
      this.primaryKeyPartNumber = primaryKeyPartNumber;
   }

}
