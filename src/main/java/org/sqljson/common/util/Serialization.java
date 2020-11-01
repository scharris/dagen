package org.sqljson.common.util;

import java.io.OutputStream;
import java.nio.file.Path;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.module.jsonSchema.JsonSchema;
import com.fasterxml.jackson.module.jsonSchema.JsonSchemaGenerator;


public final class Serialization
{
   public static <T> void writeJsonSchema
      (
         Class<T> forClass,
         OutputStream os
      )
   {
      try
      {
         ObjectMapper objMapper = new ObjectMapper();
         objMapper.registerModule(new Jdk8Module());
         objMapper.enable(SerializationFeature.INDENT_OUTPUT);
         JsonSchemaGenerator schemaGen = new JsonSchemaGenerator(objMapper);
         JsonSchema schema = schemaGen.generateSchema(forClass);
         objMapper.writeValue(os, schema);
         os.flush();
      }
      catch(Exception e)
      {
         throw new RuntimeException(e);
      }
   }

   public static ObjectMapper getObjectMapper(Path file)
   {
      String lcName = file.getFileName().toString().toLowerCase();

      if ( lcName.endsWith(".yaml") | lcName.endsWith(".yml") )
      {
         ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
         yamlMapper.registerModule(new Jdk8Module());
         return yamlMapper;
      }
      else if ( lcName.endsWith(".json") || lcName.endsWith(".json5") )
      {
         ObjectMapper jsonMapper = new JsonMapper();
         jsonMapper.registerModule(new Jdk8Module());
         return jsonMapper;
      }
      else
         throw new RuntimeException(
            "Unrecognized extension for " + file + ": expected one of 'json','yaml','yml'."
         );
   }

   private Serialization() {}
}

