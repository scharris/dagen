package gov.fda.nctr.sqljsonquery.types;


public class ChildCollectionField
{
   String name;
   GeneratedType generatedType;

   public ChildCollectionField(String name, GeneratedType generatedType)
   {
      this.name = name;
      this.generatedType = generatedType;
   }

   public String getName() { return name; }

   public GeneratedType getGeneratedType() { return generatedType; }

   @Override
   public String toString()
   {
      return "ChildCollectionField{" +
         "name='" + name + '\'' +
         ", generatedType=" + generatedType +
         '}';
   }
}