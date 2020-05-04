package org.sqljson.specs.queries;

import java.util.Collections;
import java.util.List;
import static java.util.stream.Collectors.toList;

import org.sqljson.dbmd.ForeignKey;
import org.sqljson.sql.ChildFkCondition;
import org.sqljson.sql.ParentPkCondition;


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


   public ParentPkCondition asParentPkCondition(String childAlias)
   {
      List<ForeignKey.Component> virtualForeignKeyComponents =
         equatedFields.stream()
         .map(ef -> new ForeignKey.Component(ef.childField, ef.parentPrimaryKeyField))
         .collect(toList());

      return new ParentPkCondition(childAlias, virtualForeignKeyComponents);
   }

   public ChildFkCondition asChildFkCondition(String parentAlias)
   {
      List<ForeignKey.Component> virtualForeignKeyComponents =
         equatedFields.stream()
            .map(ef -> new ForeignKey.Component(ef.childField, ef.parentPrimaryKeyField))
            .collect(toList());

      return new ChildFkCondition(parentAlias, virtualForeignKeyComponents);
   }

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
   }
}
