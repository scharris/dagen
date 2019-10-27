package org.sqljsonquery.types;


public class ChildCollectionField
{
   String name;
   GeneratedType generatedType;
   boolean nullable;

   public ChildCollectionField(String name, GeneratedType generatedType, boolean nullable)
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
   public String toString()
   {
      return "ChildCollectionField{" +
         "name='" + name + '\'' +
         ", generatedType=" + generatedType +
         ", nullable=" + nullable +
         '}';
   }
}