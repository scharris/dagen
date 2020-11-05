package org.sqljson.common.sql_dialects;

import java.util.List;

import static java.util.stream.Collectors.joining;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.sqljson.queries.sql.ColumnMetadata;

import static org.sqljson.common.util.StringFuns.*;


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
         .map(col -> "'" + unDoubleQuote(col.getName()) + "' value " + fromAlias + "." + col.getName())
         .collect(joining(",\n"));

      return
         "json_object(\n" +
            indentLines(objectFieldDecls, indentSpaces) + "\n" +
            "  returning clob\n" +
         ")";
   }

   @Override
   public String getAggregatedRowObjectsExpression
      (
         List<ColumnMetadata> columnMetadatas,
         @Nullable String orderBy,
         String fromAlias
      )
   {
      return
         "treat(coalesce(json_arrayagg(" +
            getRowObjectExpression(columnMetadatas, fromAlias) +
            (orderBy != null ? " order by " + orderBy.replace("$$", fromAlias) : "") +
            " returning clob" +
         "), to_clob('[]')) as json)";
   }

   @Override
   public String getAggregatedColumnValuesExpression
      (
         ColumnMetadata columnMetadata,
         @Nullable String orderBy,
         String fromAlias
      )
   {
      return
         "treat(coalesce(json_arrayagg(" +
            fromAlias + "." + columnMetadata.getName() +
            (orderBy != null ? " order by " + orderBy.replace("$$", fromAlias) : "") +
            " returning clob" +
         "), to_clob('[]')) as json)";
   }
}

