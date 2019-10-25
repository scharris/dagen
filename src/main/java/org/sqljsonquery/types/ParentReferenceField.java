package org.sqljsonquery.types;


public class ParentReferenceField
{
   String name;
   GeneratedType generatedType;

   public ParentReferenceField(String name, GeneratedType generatedType)
   {
      this.name = name;
      this.generatedType = generatedType;
   }

   public String getName() { return name; }

   public GeneratedType getGeneratedType() { return generatedType; }

   @Override
   public String toString()
   {
      return "ParentReferenceField{" +
         "name='" + name + '\'' +
         ", generatedType=" + generatedType +
         '}';
   }
}

