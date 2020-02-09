package org.sqljson.specs.queries;


/// An override for the type of a field in generated source code for a given programming language.
public class FieldTypeOverride
{
   private String language;
   private String typeDeclaration;

   FieldTypeOverride()
   {
      this.language = "";
      this.typeDeclaration = "";
   }

   public FieldTypeOverride(String language, String typeDeclaration)
   {
      this.language = language;
      this.typeDeclaration = typeDeclaration;
   }

   public String getLanguage() { return language; }

   public String getTypeDeclaration() { return typeDeclaration; }
}
