package org.sqljson.queries.sql.dialects;

import java.util.List;
import static java.util.stream.Collectors.joining;

import org.checkerframework.checker.nullness.qual.Nullable;

import org.sqljson.queries.sql.ColumnMetadata;
import org.sqljson.common.util.StringFuns;


public class PostgresDialect implements SqlDialect
{
   private final int indentSpaces;

   public PostgresDialect(int indentSpaces)
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
         .map(col -> "'" + StringFuns.unDoubleQuote(col.getName()) + "', " + fromAlias + "." + col.getName())
         .collect(joining(",\n"));

         return
            "jsonb_build_object(\n" +
               StringFuns.indentLines(objectFieldDecls, indentSpaces) + "\n" +
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
         "coalesce(jsonb_agg(" +
            getRowObjectExpression(columnMetadatas, fromAlias) +
            (orderBy != null ? " order by " + orderBy.replace("$$", fromAlias) : "") +
         "),'[]'::jsonb)";
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
         "coalesce(jsonb_agg(" +
            fromAlias + "." + columnMetadata.getName() +
            (orderBy != null ? " order by " + orderBy.replace("$$", fromAlias) : "") +
         "))";
   }
}

