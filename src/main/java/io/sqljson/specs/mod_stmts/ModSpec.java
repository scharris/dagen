package io.sqljson.specs.mod_stmts;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import static java.util.Optional.empty;
import static java.util.Collections.emptyList;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.sqljson.util.StringFuns;
import static io.sqljson.specs.mod_stmts.FieldParameterNameDefault.CAMELCASE_FIELDNAME;


public class ModSpec
{
   private String modificationName;
   private ModType command;
   private String tableName; // possibly qualified
   private Optional<String> tableAlias = empty();
   private Optional<String> filter = empty();
   private FieldParameterNameDefault fieldParameterNameDefault = CAMELCASE_FIELDNAME;
   private List<TableInputField> fields = emptyList();

   private ModSpec() {}

   public ModSpec
   (
      String modificationName,
      ModType command,
      String tableName,
      Optional<String> tableAlias,
      Optional<String> filter,
      FieldParameterNameDefault fieldParameterNameDefault,
      List<TableInputField> fields
   )
   {
      this.modificationName = modificationName;
      this.command = command;
      this.tableName = tableName;
      this.tableAlias = tableAlias;
      this.filter = filter;
      this.fieldParameterNameDefault = fieldParameterNameDefault;
      this.fields = fields;
   }

   public String getModificationName() { return modificationName; }

   public ModType getCommand() { return command; }

   public String getTableName() { return tableName; }

   public Optional<String> getTableAlias() { return tableAlias; }

   public Optional<String> getFilter() { return filter; }

   public FieldParameterNameDefault getFieldParameterNameDefault() { return fieldParameterNameDefault; }

   public List<TableInputField> getFields() { return fields; }


   @JsonIgnore
   public Function<String,String> getDefaultParameterNameFunction()
   {
      switch ( fieldParameterNameDefault )
      {
         case CAMELCASE_FIELDNAME: return dbFieldName -> ":" + StringFuns.lowerCamelCase(dbFieldName);
         case QUESTION_MARK: return dbFieldName -> "?";
         default: throw new RuntimeException("Unexpected parameter name default enumeration value.");
      }
   }
}
