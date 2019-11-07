package org.sqljsonquery.types;

import java.util.*;
import static java.util.Collections.unmodifiableList;

import org.sqljsonquery.dbmd.Field;
import org.sqljsonquery.query_spec.FieldTypeOverride;
import static org.sqljsonquery.util.Optionals.opt;


/// A field to be included as part of a generated type whose data source is a database column.
public class DatabaseField
{
   private String name;
   private int jdbcTypeCode;
   private String databaseType;
   private Optional<Integer> length;
   private Optional<Integer> precision;
   private Optional<Integer> fractionalDigits;
   private Optional<Boolean> nullable;
   private List<FieldTypeOverride> typeOverrides;

   DatabaseField(String name, Field dbField, List<FieldTypeOverride> typeOverrides)
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
      Optional<Integer> length,
      Optional<Integer> precision,
      Optional<Integer> fractionalDigits,
      Optional<Boolean> nullable,
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
   public Optional<Integer> getLength() { return length; }
   public Optional<Integer> getPrecision() { return precision; }
   public Optional<Integer> getFractionalDigits() { return fractionalDigits; }
   public Optional<Boolean> getNullable() { return nullable; }
   public List<FieldTypeOverride> getTypeOverrides() { return typeOverrides; }

   public Optional<FieldTypeOverride> getTypeOverride(String language)
   {
      return typeOverrides.stream().filter(to -> to.getLanguage().equals(language)).findAny();
   }

   DatabaseField toNullable()
   {
      if ( nullable.orElse(false) )
         return this;
      else
         return new DatabaseField(
            name, jdbcTypeCode, databaseType, length, precision, fractionalDigits, opt(true), typeOverrides
         );
   }

   @Override
   public boolean equals(Object o)
   {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      DatabaseField that = (DatabaseField) o;
      return jdbcTypeCode == that.jdbcTypeCode &&
         name.equals(that.name) &&
         databaseType.equals(that.databaseType) &&
         length.equals(that.length) &&
         precision.equals(that.precision) &&
         fractionalDigits.equals(that.fractionalDigits) &&
         nullable.equals(that.nullable) &&
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