package org.sqljson.util;

import java.io.OutputStream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.module.jsonSchema.JsonSchema;
import com.fasterxml.jackson.module.jsonSchema.JsonSchemaGenerator;


public final class Serialization
{
   public static <T> void writeJsonSchema(Class<T> forClass, OutputStream os)
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

   private Serialization() {}
}
