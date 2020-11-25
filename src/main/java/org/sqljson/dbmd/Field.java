package org.sqljson.dbmd;

import java.sql.Types;
import static java.util.Objects.requireNonNull;

import org.checkerframework.checker.nullness.qual.Nullable;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({
   "name", "databaseType", "nullable", "primaryKeyPartNumber", "length", "precision", "precisionRadix"
})
public class Field {

   private String name;

   private int jdbcTypeCode;

   private String databaseType;

   private @Nullable Integer length;

   private @Nullable Integer precision;

   private @Nullable Integer precisionRadix;

   private @Nullable Integer fractionalDigits;

   private @Nullable Boolean nullable;

   private @Nullable Integer primaryKeyPartNumber;


   public Field
      (
         String name,
         int jdbcTypeCode,
         String databaseType,
         @Nullable Integer length,
         @Nullable Integer precision,
         @Nullable Integer precisionRadix,
         @Nullable Integer fractionalDigits,
         @Nullable Boolean nullable,
         @Nullable Integer primaryKeyPartNumber
      )
   {
      this.name = requireNonNull(name);
      this.jdbcTypeCode = jdbcTypeCode;
      this.databaseType = requireNonNull(databaseType);
      this.length = length;
      this.precision = precision;
      this.precisionRadix = precisionRadix;
      this.fractionalDigits = fractionalDigits;
      this.nullable = nullable;
      this.primaryKeyPartNumber = primaryKeyPartNumber;
   }

   Field()
   {
      this.name = "";
      this.databaseType = "";
   }

   public String getName() { return name; }

   public int getJdbcTypeCode() { return jdbcTypeCode; }

   public String getDatabaseType() { return databaseType; }

   public @Nullable Integer getLength() { return length; }

   public @Nullable Integer getFractionalDigits() { return fractionalDigits; }

   public @Nullable Integer getPrecision() { return precision; }

   public @Nullable Integer getPrecisionRadix() { return precisionRadix; }

   public @Nullable Boolean getNullable() { return nullable; }

   public @Nullable Integer getPrimaryKeyPartNumber() { return primaryKeyPartNumber; }

   @JsonIgnore
   public boolean isNumericType() { return isJdbcTypeNumeric(jdbcTypeCode); }

   @JsonIgnore
   public boolean isCharacterType() { return isJdbcTypeChar(jdbcTypeCode); }

   public static boolean isJdbcTypeNumeric(int jdbcType)
   {
      switch ( jdbcType )
      {
         case Types.TINYINT:
         case Types.SMALLINT:
         case Types.INTEGER:
         case Types.BIGINT:
         case Types.FLOAT:
         case Types.REAL:
         case Types.DOUBLE:
         case Types.DECIMAL:
         case Types.NUMERIC:
            return true;
         default:
            return false;
      }
   }

   public static boolean isJdbcTypeChar(int jdbcType)
   {
      switch (jdbcType)
      {
         case Types.CHAR:
         case Types.VARCHAR:
         case Types.LONGVARCHAR:
            return true;
         default:
            return false;
      }
   }
}