package org.sqljsonquery.types;

import java.util.Objects;
import java.util.Optional;

import org.sqljsonquery.dbmd.Field;

import static org.sqljsonquery.util.Optionals.opt;


public class DatabaseField
{
   String name;
   int jdbcTypeCode;
   String databaseType;
   Optional<Integer> length;
   Optional<Integer> precision;
   Optional<Integer> fractionalDigits;
   Optional<Boolean> nullable;

   public DatabaseField(String name, Field dbField)
   {
      this.name = name;
      this.jdbcTypeCode = dbField.getJdbcTypeCode();
      this.databaseType = dbField.getDatabaseType();
      this.length = dbField.getLength();
      this.precision = dbField.getPrecision();
      this.fractionalDigits = dbField.getFractionalDigits();
      this.nullable = dbField.getNullable();
   }

   public DatabaseField
   (
      String name,
      int jdbcTypeCode,
      String databaseType,
      Optional<Integer> length,
      Optional<Integer> precision,
      Optional<Integer> fractionalDigits,
      Optional<Boolean> nullable
   )
   {
      this.name = name;
      this.jdbcTypeCode = jdbcTypeCode;
      this.databaseType = databaseType;
      this.length = length;
      this.precision = precision;
      this.fractionalDigits = fractionalDigits;
      this.nullable = nullable;
   }

   public String getName() { return name; }
   public int getJdbcTypeCode() { return jdbcTypeCode; }
   public String getDatabaseType() { return databaseType; }
   public Optional<Integer> getLength() { return length; }
   public Optional<Integer> getPrecision() { return precision; }
   public Optional<Integer> getFractionalDigits() { return fractionalDigits; }
   public Optional<Boolean> getNullable() { return nullable; }

   public DatabaseField toNullable()
   {
      if ( nullable.orElse(false) ) return this;
      else return new DatabaseField(name, jdbcTypeCode, databaseType, length, precision, fractionalDigits, opt(true));
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
         nullable.equals(that.nullable);
   }

   @Override
   public int hashCode()
   {
      return Objects.hash(name, jdbcTypeCode, databaseType, length, precision, fractionalDigits, nullable);
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
         '}';
   }
}