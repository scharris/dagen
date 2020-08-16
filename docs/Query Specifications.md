# Query Specifications
```
defaultSchema: <db schema name>
querySpecs:
  - queryName: <query name>
    tableJson:
      <TABLE-JSON>
  ... # any number of queries listed here
```

Where `<TABLE-JSON>` is of the form:

```
table: <table name> # (optionally can be qualified)
fieldExpressions:
  - field: <field-name> # simple field value from the table
    jsonProperty: <property name> # optional, defaults to camelcase form of field name
  ...
  - expression: <expr val>  # Expression derived from fields, use $$ as table alias for field references.
    jsonProperty: <property name>
    generatedFieldType: <Java type declaration>
  ...
inlineParentTables: # "Inline" parent tables have their fields included directly with the current table's own fields.
  - tableJson:
      ...
  ...
referencedParentTables: # Referenced parent table records are wrapped in a json object.
  - referenceName: <json field name>
    tableJson:
      ...
  ...
childTableCollections:
  - collectionName: <json field name>
    tableJson:
      ...
  ...
```

TODO: Show how to customize a json property name (and that default is camel-cased db field name).

TODO: Show how to customize the generated Java type for a field.

TODO: Show how to include an expression field (use an example depending on table field).
      Expression fields must specify the json property name, and the generated type for the field if generating types.
```      
        - expression: "$$.first_name || ' ' || $$.last_name"
          jsonProperty: fullName 
          generatedFieldType: @Nullable String
```      

TODO: Show how to reference a parent table for which multiple fk's exist from the current table.
```
      viaForeignKeyFields:
        - compound_id
```
            
TODO: Show how to add a general record condition.
```
      recordCondition:
        sql: 'not $$.id = 2'
```

TODO: Show how to add a condition based on a field value using a standard operator.
      This has the advantage of checking database metadata for the existence of the
      field, and generating a source code constant representing the parameter to
      prevent errors.
```
      fieldParamConditions:
        - field: mesh_id
          op: IN
```


