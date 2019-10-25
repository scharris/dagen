package org.sqljsonquery.types;


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

