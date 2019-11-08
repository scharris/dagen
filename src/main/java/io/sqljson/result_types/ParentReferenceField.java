package io.sqljson.result_types;

import java.util.Objects;


public class ParentReferenceField
{
   String name;
   GeneratedType generatedType;
   boolean nullable;

   public ParentReferenceField
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

   public ParentReferenceField toNullable()
   {
      if ( nullable ) return this;
      else return new ParentReferenceField(name, generatedType, true);
   }

   @Override
   public boolean equals(Object o)
   {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      ParentReferenceField that = (ParentReferenceField) o;
      return nullable == that.nullable &&
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
      return "ParentReferenceField{" +
         "name='" + name + '\'' +
         ", generatedType=" + generatedType +
         ", isNullable=" + nullable +
         '}';
   }
}

