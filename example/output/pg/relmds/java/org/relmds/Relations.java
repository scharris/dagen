// ---------------------------------------------------------------------------
// [ THIS SOURCE CODE WAS AUTO-GENERATED, ANY CHANGES MADE HERE MAY BE LOST. ]
// ---------------------------------------------------------------------------
package org.relmds;

public class Relations
{

   public static class drugs
   {

      public static class advisory
      {
         public static String id() { return "drugs.advisory"; }
         public static final Field id = new Field("id",4,"int4",null,10,0,false,1);
         public static final Field drug_id = new Field("drug_id",4,"int4",null,10,0,false,null);
         public static final Field advisory_type_id = new Field("advisory_type_id",4,"int4",null,10,0,false,null);
         public static final Field text = new Field("text",12,"varchar",2000,null,null,false,null);
      }

      public static class advisory_type
      {
         public static String id() { return "drugs.advisory_type"; }
         public static final Field id = new Field("id",4,"int4",null,10,0,false,1);
         public static final Field name = new Field("name",12,"varchar",50,null,null,false,null);
         public static final Field authority_id = new Field("authority_id",4,"int4",null,10,0,false,null);
      }

      public static class analyst
      {
         public static String id() { return "drugs.analyst"; }
         public static final Field id = new Field("id",4,"int4",null,10,0,false,1);
         public static final Field short_name = new Field("short_name",12,"varchar",50,null,null,false,null);
      }

      public static class authority
      {
         public static String id() { return "drugs.authority"; }
         public static final Field id = new Field("id",4,"int4",null,10,0,false,1);
         public static final Field name = new Field("name",12,"varchar",200,null,null,false,null);
         public static final Field url = new Field("url",12,"varchar",500,null,null,true,null);
         public static final Field description = new Field("description",12,"varchar",2000,null,null,true,null);
         public static final Field weight = new Field("weight",4,"int4",null,10,0,true,null);
      }

      public static class brand
      {
         public static String id() { return "drugs.brand"; }
         public static final Field drug_id = new Field("drug_id",4,"int4",null,10,0,false,1);
         public static final Field brand_name = new Field("brand_name",12,"varchar",200,null,null,false,2);
         public static final Field language_code = new Field("language_code",12,"varchar",10,null,null,true,null);
         public static final Field manufacturer_id = new Field("manufacturer_id",4,"int4",null,10,0,true,null);
      }

      public static class compound
      {
         public static String id() { return "drugs.compound"; }
         public static final Field id = new Field("id",4,"int4",null,10,0,false,1);
         public static final Field display_name = new Field("display_name",12,"varchar",50,null,null,true,null);
         public static final Field nctr_isis_id = new Field("nctr_isis_id",12,"varchar",100,null,null,true,null);
         public static final Field smiles = new Field("smiles",12,"varchar",2000,null,null,true,null);
         public static final Field canonical_smiles = new Field("canonical_smiles",12,"varchar",2000,null,null,true,null);
         public static final Field cas = new Field("cas",12,"varchar",50,null,null,true,null);
         public static final Field mol_formula = new Field("mol_formula",12,"varchar",2000,null,null,true,null);
         public static final Field mol_weight = new Field("mol_weight",2,"numeric",null,131089,0,true,null);
         public static final Field entered = new Field("entered",93,"timestamptz",null,null,null,true,null);
         public static final Field entered_by = new Field("entered_by",4,"int4",null,10,0,false,null);
      }

      public static class drug
      {
         public static String id() { return "drugs.drug"; }
         public static final Field id = new Field("id",4,"int4",null,10,0,false,1);
         public static final Field name = new Field("name",12,"varchar",500,null,null,false,null);
         public static final Field compound_id = new Field("compound_id",4,"int4",null,10,0,false,null);
         public static final Field mesh_id = new Field("mesh_id",12,"varchar",7,null,null,true,null);
         public static final Field drugbank_id = new Field("drugbank_id",12,"varchar",7,null,null,true,null);
         public static final Field cid = new Field("cid",4,"int4",null,10,0,true,null);
         public static final Field therapeutic_indications = new Field("therapeutic_indications",12,"varchar",4000,null,null,true,null);
         public static final Field registered = new Field("registered",93,"timestamptz",null,null,null,true,null);
         public static final Field registered_by = new Field("registered_by",4,"int4",null,10,0,false,null);
         public static final Field market_entry_date = new Field("market_entry_date",91,"date",null,null,null,true,null);
      }

      public static class drug_functional_category
      {
         public static String id() { return "drugs.drug_functional_category"; }
         public static final Field drug_id = new Field("drug_id",4,"int4",null,10,0,false,1);
         public static final Field functional_category_id = new Field("functional_category_id",4,"int4",null,10,0,false,2);
         public static final Field authority_id = new Field("authority_id",4,"int4",null,10,0,false,3);
         public static final Field seq = new Field("seq",4,"int4",null,10,0,true,null);
      }

      public static class drug_reference
      {
         public static String id() { return "drugs.drug_reference"; }
         public static final Field drug_id = new Field("drug_id",4,"int4",null,10,0,false,1);
         public static final Field reference_id = new Field("reference_id",4,"int4",null,10,0,false,2);
         public static final Field priority = new Field("priority",4,"int4",null,10,0,true,null);
      }

      public static class functional_category
      {
         public static String id() { return "drugs.functional_category"; }
         public static final Field id = new Field("id",4,"int4",null,10,0,false,1);
         public static final Field name = new Field("name",12,"varchar",500,null,null,false,null);
         public static final Field description = new Field("description",12,"varchar",2000,null,null,true,null);
         public static final Field parent_functional_category_id = new Field("parent_functional_category_id",4,"int4",null,10,0,true,null);
      }

      public static class manufacturer
      {
         public static String id() { return "drugs.manufacturer"; }
         public static final Field id = new Field("id",4,"int4",null,10,0,false,1);
         public static final Field name = new Field("name",12,"varchar",200,null,null,false,null);
      }

      public static class reference
      {
         public static String id() { return "drugs.reference"; }
         public static final Field id = new Field("id",4,"int4",null,10,0,false,1);
         public static final Field publication = new Field("publication",12,"varchar",2000,null,null,false,null);
      }

   }

}
