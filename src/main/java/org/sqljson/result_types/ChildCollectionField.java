package org.sqljson.result_types;

import java.util.Objects;

import org.checkerframework.checker.nullness.qual.Nullable;


public class ChildCollectionField
{
   String name;
   GeneratedType generatedType;
   boolean nullable;

   public ChildCollectionField
      (
         String name,
         GeneratedType generatedType,
         boolean nullable
      )
   {
      this.name = name;
      this.generatedType = generatedType;
      this.nullable = nullable;
   }

   public String getName() { return name; }

   public GeneratedType getGeneratedType() { return generatedType; }

   public boolean isNullable() { return nullable; }

   public ChildCollectionField toNullable()
   {
      if ( nullable ) return this;
      else return new ChildCollectionField(name, generatedType, true);
   }

   @Override
   public boolean equals(@Nullable Object o)
   {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      ChildCollectionField that = (ChildCollectionField) o;
      return
         nullable == that.nullable &&
         name.equals(that.name) &&
         generatedType.equals(that.generatedType);
   }

   @Override
   public int hashCode()
   {
      return Objects.hash(name, generatedType, nullable);
   }

   @Override
   public String toString()
   {
      return "ChildCollectionField{" +
         "name='" + name + '\'' +
         ", generatedType=" + generatedType +
         ", nullable=" + nullable +
         '}';
   }
}
