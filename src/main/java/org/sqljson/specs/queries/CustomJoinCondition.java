package org.sqljson.specs.queries;

import org.sqljson.dbmd.DatabaseMetadata;
import org.sqljson.sql.ParentChildCondition;

import static org.sqljson.util.StringFuns.replaceStringsInWith;


public class CustomJoinCondition
{
   private final String withParentAliasAs; // pattern representing parent table alias in joinExpression, e.g. "$p"
   private final String withChildAliasAs;  // pattern representing child table alias in joinExpression, e.g. "$c"
   private final String joinExpression;    // join expression, e.g. "$p.field1 = $c.field2 and $p.field3 = $c.field4"

   private CustomJoinCondition()
   {
      this.withParentAliasAs = "";
      this.withChildAliasAs = "";
      this.joinExpression = "";
   }

   public CustomJoinCondition
      (
         String withParentAliasAs,
         String withChildAliasAs,
         String joinExpression
      )
   {
      this.withParentAliasAs = withParentAliasAs;
      this.withChildAliasAs = withChildAliasAs;
      this.joinExpression = joinExpression;
   }

   public String getWithParentAliasAs() { return withParentAliasAs; }

   public String getWithChildAliasAs() { return withChildAliasAs; }

   public String getJoinExpression() { return joinExpression; }


   public ParentChildCondition asConditionOnChildForParentAlias(String parentAlias)
   {
      return new ParentChildCondition()
      {
         public String asEquationConditionOn(String childAlias, DatabaseMetadata dbmd)
         {
            return replaceStringsInWith(joinExpression,
               withParentAliasAs, parentAlias,
               withChildAliasAs, childAlias
            );
         }

         public String getOtherTableAlias()
         {
            return parentAlias;
         }
      };
   }

   public ParentChildCondition asConditionOnParentForChildAlias(String childAlias)
   {
      return new ParentChildCondition()
      {
         public String asEquationConditionOn(String parentAlias, DatabaseMetadata dbmd)
         {
            return replaceStringsInWith(joinExpression,
               withParentAliasAs, parentAlias,
               withChildAliasAs, childAlias
            );
         }

         public String getOtherTableAlias()
         {
            return childAlias;
         }
      };
   }
}
