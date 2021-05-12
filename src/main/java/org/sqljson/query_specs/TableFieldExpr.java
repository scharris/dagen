package org.sqljson.query_specs;

import java.io.IOException;

import org.checkerframework.checker.nullness.qual.Nullable;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.node.JsonNodeType;


@JsonDeserialize(using = TableFieldExprDeserializer.class) // Allow deserializing from simple String as "field" property.
public final class TableFieldExpr
{
   private @Nullable String field;
   private @Nullable String expression;
   private @Nullable String withTableAliasAs;
   private @Nullable String jsonProperty;
   private @Nullable String fieldTypeInGeneratedSource;

   private TableFieldExpr() {}

   public TableFieldExpr
      (
         @Nullable String field,
         @Nullable String expression,
         @Nullable String withTableAliasAs,
         @Nullable String jsonProperty,
         @Nullable String fieldTypeInGeneratedSource
      )
   {
      this.field = field;
      this.expression = expression;
      this.withTableAliasAs = withTableAliasAs;
      this.jsonProperty = jsonProperty;
      this.fieldTypeInGeneratedSource = fieldTypeInGeneratedSource;

      if ( (field != null) == (expression != null) )
         throw new RuntimeException("Exactly one of database field name and value expression should be specified.");
      if ( withTableAliasAs != null && expression == null )
         throw new RuntimeException("Cannot specify withTableAliasAs without expression value.");
   }

   public @Nullable String getField() { return field; }

   public @Nullable String getExpression() { return expression; }

   public @Nullable String getWithTableAliasAs() { return withTableAliasAs; }

   public @Nullable String getJsonProperty() { return jsonProperty; }

   public @Nullable String getFieldTypeInGeneratedSource() { return fieldTypeInGeneratedSource; }
}

/// Allow simple String to be deserialized to a TableFieldExpression with the value as the "field"
/// property and other values null.
class TableFieldExprDeserializer extends JsonDeserializer<TableFieldExpr>
{
   @Override
   public TableFieldExpr deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException
   {
      JsonNode node = jsonParser.readValueAs(JsonNode.class);

      if ( node.getNodeType() == JsonNodeType.STRING )
         return new TableFieldExpr(node.textValue(), null, null, null, null);
      else
      {
         @Nullable String field = node.has("field") ? node.get("field").textValue(): null;
         @Nullable String expr = node.has("expression") ? node.get("expression").textValue(): null;
         @Nullable String withTableAliasAs = node.has("withTableAliasAs") ? node.get("withTableAliasAs").textValue(): null;
         @Nullable String jsonProperty = node.has("jsonProperty") ? node.get("jsonProperty").textValue(): null;
         @Nullable String genFieldType = node.has("fieldTypeInGeneratedSource") ? node.get("fieldTypeInGeneratedSource").textValue(): null;
         return new TableFieldExpr(field, expr, withTableAliasAs, jsonProperty, genFieldType);
      }
   }
}
