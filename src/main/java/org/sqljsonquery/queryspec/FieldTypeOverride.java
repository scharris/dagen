package org.sqljsonquery.queryspec;


/// An override for the type of a field in generated source code for a given programming language.
public class FieldTypeOverride
{
   private String language;
   private String typeDeclaration;

   private FieldTypeOverride() {}

   public FieldTypeOverride(String language, String typeDeclaration)
   {
      this.language = language;
      this.typeDeclaration = typeDeclaration;
   }

   public String getLanguage() { return language; }

   public String getTypeDeclaration() { return typeDeclaration; }
}
