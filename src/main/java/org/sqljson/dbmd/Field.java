package org.sqljson.dbmd;

import java.sql.Types;
import static java.util.Objects.requireNonNull;

import org.checkerframework.checker.nullness.qual.Nullable;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({
   "name", "databaseType", "nullable", "primaryKeyPartNumber", "comment"
})
public class Field {

   private String name;

   private int jdbcTypeCode;

   private String databaseType;

   private @Nullable Integer length;

   private @Nullable Integer precision;

   private @Nullable Integer fractionalDigits;

   private @Nullable Boolean nullable;

   private @Nullable Integer primaryKeyPartNumber;

   private @Nullable String comment;

   public Field
      (
         String name,
         int jdbcTypeCode,
         String databaseType,
         @Nullable Integer length,
         @Nullable Integer precision,
         @Nullable Integer fractionalDigits,
         @Nullable Boolean nullable,
         @Nullable Integer primaryKeyPartNumber,
         @Nullable String comment
      )
   {
      this.name = requireNonNull(name);
      this.jdbcTypeCode = jdbcTypeCode;
      this.databaseType = requireNonNull(databaseType);
      this.length = length;
      this.precision = precision;
      this.fractionalDigits = fractionalDigits;
      this.nullable = nullable;
      this.primaryKeyPartNumber = primaryKeyPartNumber;
      this.comment = comment;
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

   public @Nullable Boolean getNullable() { return nullable; }

   public @Nullable Integer getPrimaryKeyPartNumber() { return primaryKeyPartNumber; }

   public @Nullable String getComment() { return comment; }

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