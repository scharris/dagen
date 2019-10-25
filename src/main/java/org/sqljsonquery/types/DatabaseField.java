package org.sqljsonquery.types;

import java.util.Optional;

import gov.fda.nctr.dbmd.Field;


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

   public String getName() { return name; }
   public int getJdbcTypeCode() { return jdbcTypeCode; }
   public String getDatabaseType() { return databaseType; }
   public Optional<Integer> getLength() { return length; }
   public Optional<Integer> getPrecision() { return precision; }
   public Optional<Integer> getFractionalDigits() { return fractionalDigits; }
   public Optional<Boolean> getNullable() { return nullable; }

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