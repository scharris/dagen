package org.sqljson.result_types;

import java.util.*;
import static java.util.Collections.unmodifiableList;
import org.checkerframework.checker.nullness.qual.Nullable;

import org.sqljson.dbmd.Field;
import org.sqljson.specs.queries.FieldTypeOverride;


/// A field to be included as part of a generated type whose data source is a database column.
public class DatabaseField
{
   private String name;
   private int jdbcTypeCode;
   private String databaseType;
   private @Nullable Integer length;
   private @Nullable Integer precision;
   private @Nullable Integer fractionalDigits;
   private @Nullable Boolean nullable;
   private List<FieldTypeOverride> typeOverrides;

   DatabaseField
      (
         String name,
         Field dbField,
         List<FieldTypeOverride> typeOverrides
      )
   {
      this.name = name;
      this.jdbcTypeCode = dbField.getJdbcTypeCode();
      this.databaseType = dbField.getDatabaseType();
      this.length = dbField.getLength();
      this.precision = dbField.getPrecision();
      this.fractionalDigits = dbField.getFractionalDigits();
      this.nullable = dbField.getNullable();
      this.typeOverrides = unmodifiableList(new ArrayList<>(typeOverrides));
   }

   private DatabaseField
      (
         String name,
         int jdbcTypeCode,
         String databaseType,
         @Nullable Integer length,
         @Nullable Integer precision,
         @Nullable Integer fractionalDigits,
         @Nullable Boolean nullable,
         List<FieldTypeOverride> typeOverrides
      )
   {
      this.name = name;
      this.jdbcTypeCode = jdbcTypeCode;
      this.databaseType = databaseType;
      this.length = length;
      this.precision = precision;
      this.fractionalDigits = fractionalDigits;
      this.nullable = nullable;
      this.typeOverrides = typeOverrides;
   }

   public String getName() { return name; }
   public int getJdbcTypeCode() { return jdbcTypeCode; }
   public String getDatabaseType() { return databaseType; }
   public @Nullable Integer getLength() { return length; }
   public @Nullable Integer getPrecision() { return precision; }
   public @Nullable Integer getFractionalDigits() { return fractionalDigits; }
   public @Nullable Boolean getNullable() { return nullable; }
   public List<FieldTypeOverride> getTypeOverrides() { return typeOverrides; }

   public @Nullable FieldTypeOverride getTypeOverride(String language)
   {
      return
         typeOverrides.stream()
         .filter(to -> to.getLanguage().equals(language))
         .findAny()
         .orElse(null);
   }

   DatabaseField toNullable()
   {
      if ( nullable != null && nullable )
         return this;
      else
         return new DatabaseField(
            name, jdbcTypeCode, databaseType, length, precision, fractionalDigits, true, typeOverrides
         );
   }

   @Override
   public boolean equals(@Nullable Object o)
   {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      DatabaseField that = (DatabaseField) o;
      return
         jdbcTypeCode == that.jdbcTypeCode &&
         name.equals(that.name) &&
         databaseType.equals(that.databaseType) &&
         Objects.equals(length, that.length) &&
         Objects.equals(precision, that.precision) &&
         Objects.equals(fractionalDigits, that.fractionalDigits) &&
         Objects.equals(nullable, that.nullable) &&
         typeOverrides.equals(that.typeOverrides);
   }

   @Override
   public int hashCode()
   {
      return Objects.hash(name, jdbcTypeCode, databaseType, length, precision, fractionalDigits, nullable, typeOverrides);
   }

   @Override
   public String toString()
   {
      return "DatabaseField{" +
         "name='" + name + '\'' +
         ", jdbcTypeCode=" + jdbcTypeCode +
         ", databaseType='" + databaseType + '\'' +
         ", length=" + length +
         ", precision=" + precision +
         ", fractionalDigits=" + fractionalDigits +
         ", nullable=" + nullable +
         ", typeOverrides=" + typeOverrides +
         '}';
   }
}
