package io.sqljson.sql.dialect;

import java.util.List;
import static java.util.stream.Collectors.joining;

import io.sqljson.sql.ColumnMetadata;
import static io.sqljson.sql.SelectClauseEntry.Source.CHILD_COLLECTION;
import static io.sqljson.util.StringFuns.indentLines;
import static io.sqljson.util.StringFuns.unDoubleQuote;


public class OracleDialect implements SqlDialect
{
   private final int indentSpaces;

   public OracleDialect(int indentSpaces)
   {
      this.indentSpaces = indentSpaces;
   }

   @Override
   public String getRowObjectExpression
   (
      List<ColumnMetadata> columnMetadatas,
      String fromAlias
   )
   {
      String objectFieldDecls =
         columnMetadatas.stream()
         .map(col -> "'" + unDoubleQuote(col.getOutputName()) + "' value " +
            (col.getSource() == CHILD_COLLECTION ?
               "treat(" + fromAlias + "." + col.getOutputName() + " as json)"
               :  fromAlias + "." + col.getOutputName())
         )
         .collect(joining(",\n"));

      return
         "json_object(\n" +
            indentLines(objectFieldDecls, indentSpaces) + "\n" +
         ")";
   }

   @Override
   public String getAggregatedRowObjectsExpression
   (
      List<ColumnMetadata> columnMetadatas,
      String fromAlias
   )
   {
      return
         "json_arrayagg(" +
            getRowObjectExpression(columnMetadatas, fromAlias) +
         " returning clob)";
   }

   /// Replace empty clob returned by json_arrayagg() when aggregating over no
   /// source rows with empty json array.
   /* NOTE
   Oracle unfortunately has json_arrayagg() return an empty clob when aggregating
   over no rows, whereas we need an empty json array value in that case. Since an
   empty clob is non-null, it's not easy to replace at the level of the aggregate
   select expression, without introducing a new function in the schema which is
   to be avoided. So we resort to wrapping the aggregate query in another query
   here, which can replace the faulty empty aggregate representation.
   */
   @Override
   public String getAggregatedObjectsFinalQuery(String simpleAggregatedObjectsQuery, String jsonValueColumnName)
   {
      String origAggVal = "q." + jsonValueColumnName;

      return
         "select case when dbms_lob.getlength(" + origAggVal + ") = 0 then to_clob('[]') " +
         "else " + origAggVal + " end " +
         "from (\n" +
            indentLines(simpleAggregatedObjectsQuery, 2) + "\n" +
         ") q";
   }
}
