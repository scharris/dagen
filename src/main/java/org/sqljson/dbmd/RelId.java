package org.sqljson.dbmd;

import java.util.Objects;
import static java.util.Objects.requireNonNull;

import org.checkerframework.checker.nullness.qual.Nullable;

import com.fasterxml.jackson.annotation.JsonIgnore;

public final class RelId {

   static RelId DUMMY_INSTANCE = new RelId();

   private @Nullable String schema;

   private String name;

   public RelId
      (
         @Nullable String schema,
         String name
      )
   {
      this.schema = schema;
      this.name = requireNonNull(name);
   }

   RelId()
   {
      this.name = "";
   }

   public @Nullable String getSchema() { return schema; }

   public String getName() { return name; }


   public String toString()
   {
      return getIdString();
   }

   @JsonIgnore
   public String getIdString()
   {
      return (schema != null ? schema + "." : "") + name;
   }


   public boolean equals(@Nullable Object other)
   {
      if ( !(other instanceof RelId) )
         return false;
      else
      {
         RelId o = (RelId)other;
         return
            Objects.equals(schema, o.schema) &&
               Objects.equals(name, o.name);
      }
   }

   public int hashCode()
   {
      return (schema != null ? schema.hashCode() : 0)  + 7 * name.hashCode();
   }
}
