package org.sqljson.queries.specs;

import java.util.Collections;
import java.util.List;


public class CustomJoinCondition
{
   private final List<FieldPair> equatedFields;

   private CustomJoinCondition()
   {
      equatedFields = Collections.emptyList();
   }

   public CustomJoinCondition(List<FieldPair> equatedFields)
   {
      this.equatedFields = equatedFields;
   }

   public List<FieldPair> getEquatedFields() { return equatedFields; }


   public static class FieldPair
   {
      private final String childField;

      private final String parentPrimaryKeyField;

      public FieldPair(String childField, String parentPrimaryKeyField)
      {
         this.childField = childField;
         this.parentPrimaryKeyField = parentPrimaryKeyField;
      }

      private FieldPair()
      {
         this.childField = "";
         this.parentPrimaryKeyField = "";
      }

      public String getChildField() { return childField; }

      public String getParentPrimaryKeyField() { return parentPrimaryKeyField; }

      @Override
      public String toString()
      {
         return "FieldPair{" +
            "childField='" + childField + '\'' +
            ", parentPrimaryKeyField='" + parentPrimaryKeyField + '\'' +
            '}';
      }
   }

   @Override
   public String toString()
   {
      return "CustomJoinCondition{" +
         "equatedFields=" + equatedFields +
         '}';
   }
}
