# Query Specifications File
The query specifications file is a YAML or JSON formatted file containing
descriptions of queries to be generated. Each query describes a graph of tables
or views used to build json result values. Its content should conform to the
json schema definition provided in file `editor-config/query-specs-schema.json`.
To prevent errors, during query generation, any references to database tables
and fields are verified against database metadata, as are the existence of any
foreign keys (either referenced explicitly or implicitly) supporting
parent/child table relationships. What follows is a detailed description of the
structure and properties of the query specifications file.

## Schema information
```
defaultSchema: <db schema name>
```
This line sets the default schema to be assumed for any unqualified object
references in the query specifications. The schema name must be known by
the query generator in order to lookup information about database objects
in database metadata for the query generation process.

```
generateUnqualifiedNamesForSchemas: [<schema-name>[,...]]
```
This optional item enables generation of unqualified names in the generated SQL
for any objects in the listed schemas. It should always be OK to leave this
unset, but its inclusion can make the generated SQL shorter and easier to read
if your are directly connecting to a given schema where qualified names or not
necessary, or your database/connections support a "search-path" of schemas. 

## Query specifications
```
querySpecs:
```
The list of specifications for the queries to be generated.

Each query specification entry in `querySpecs` should have the form:
```
  - queryName: <query name>
    resultRepresentations: [<result reprs>]
    generateResultTypes: true | false
    tableJson:
        <TABLE-JSON-SPEC>
```

Here `queryName` assigns a unique name for the query, from which the name of the
generated SQL resource file is derived, as well as the Java class name containing
result types and query parameter information. A name of 'my query' for example
will generate a SQL resource file named `my query(<results repr>).sql` for each
result representation (results representations are described below). If types
generation is enabled then a Java class named MyQuery is also produced.
The Java class also contains the name of the generated resource file in a
static member, so in code it's best to obtain the SQL resource file name from
this class member to prevent runtime errors. 

`resultRepresentation` is a list of results representations to be generated,
with one SQL file produced for each representation. The choices are as follows:

  - JSON_OBJECT_ROWS (the default)
  
    In this representation, each result record of the top-most table in the
    query is represented by a result row having one json-valued column. Within
    each json value will be any fields specified in the top table and any nested
    data from rows of other tables which are related to the given row as
    specified in the query specification.
    
  - JSON_ARRAY_ROW
  
    With this representation, the SQL query will yield a single json array value
    in result set of just one row and column. Within the json array are json
    objects representing all result rows of the top level table, together with
    any nested data specified from related tables as specified in the query 
    specification.
    
  - MULTI_COLUMN_ROWS
  
    In this representation, the generated SQL can yield multiple rows and
    multiple columns. The columns of the result rows are those selected from 
    the top level table itself plus any columns representing related parent and
    child records.

`generateResultTypes`
This field controls whether to generate source code (e.g. Java) for result
types for this query. Defaults to true.

### The table json specification

The `<TABLE-JSON-SPEC>` structure, assigned to field `tableJson` in the query
specification, describes the JSON output for a top-level table and possibly also
nested content from related parent and child tables. It can occur at any number
of points within the overall query structure, aside from its mandatory usage at
the top level of the query described above. It has one required and several
optional properties. The required property is the table name:
```
        table: <table name> # (optionally can be qualified)
```
which provides the name of the table or view providing data at the top of the
object graph to be fetched by this query.

Following the table name is usually a list of table fields and field expressions
involving the table fields which are to be included in the output from the top
table.
```
        fieldExpressions:
          - [field: <field-name> | expression: <value expression with $$ as table alias>]
            [jsonProperty: <property-name>]               # (required for expressions)
            [fieldTypeInGeneratedSource: <Java type declaration>] # (required for expressions)
          ...
```

Here the field name or expression value, provided via `field` or `expression`
in the first line of each array entry, provides the value for the corresponding
property in the output object. The `jsonProperty` provides the output property
name for the value. If result types are being generated (e.g. in Java), then 
`fieldTypeInGeneratedSource` can be used to set the type declaration to be used for the
field member in the generated source code for result types. The type declaration
can include decorations on the type such as annotations as well, for example
`@NotNull String`.

For the `field` variant, both `jsonProperty` and `fieldTypeInGeneratedSource` are
optional, as they can be derived from the database table field name
(transformed to camelCase) and from its database type, respectively. Expression
fields require these properties however, because otherwise there would be no way
to determine the output property name, and no practical way to determine the
type to be generated. An example of an expression field entry is given
below:
```      
        - expression: "$$.first_name || ' ' || $$.last_name"
          jsonProperty: fullName 
          fieldTypeInGeneratedSource: @Nullable String
```
Here the occurrences of `$$` in the expression value stand for the table alias
which is automatically generated for the table specified in the `table` field.
Qualifying field names with `$$` is usually not necessary, but can sometimes
prevent ambiguities when using subqueries within expressions.

A simple field entry on the other hand can be as simple as:
```
        - field: mesh_id
```
Note that since `jsonProperty` is not specified, it will default to the
camelcase form of the field name, in this case `meshId`.  The generated type
will also default to a type derived from the database field type, as obtained
from database metadata.

Following the fields and expressions that come from the top table directly, next
we can include field values for the output obtained from related parent and
child tables through one or more of several optional properties that are
described below. These properties give a lot of flexibility because each allows
inclusion of their own full `tableJson` specifications, recursively.

For parent tables we have the option of including the parent's result fields
*inline* together with the top table's own fields, via an entry not having 
a reference name, as follows:
```
        parentTables: # "Inline" parent tables have their fields included directly with the current table's own fields.
          - tableJson:
              <TABLE-JSON-SPEC>
          ...
```
Here `<TABLE-JSON-SPEC>` can be any full table json specification using the 
parent table as top table, which can include fields from its own related parent
and child tables. The fields from the parent's `tableJson` specification will be
appended to current table's json specification fields as if they had originated
from the current table itself.

Any number of parent tables whose fields are to be included can be specified
under the `parentTables` property.

The other option for including parent table data is to specify a single field
to reference a parent object containing its field data. This is done via the
same `parentTables` property but with referenceName specified:
```
        parentTables:
          - referenceName: <json field name>
            tableJson:
              <TABLE-JSON-SPEC>
          ...
```

For this variant, the reference field name must be provided. As before, any
number of parent tables can be specified here.


Finally, data from any number of child tables can be included as child
collections:
```
        childTableCollections:
          - collectionName: <json field name>
            tableJson:
              <TABLE-JSON-SPEC>
          ...
```
Data for a child table is always represented as a collection (Json array), and
a name for the collection must be provided in `collectionName`.  Each child
collection can include any table json specification, including additional parent
and child tables, etc., to any depth.



TODO: Show how to reference a parent table for which multiple fk's exist from the current table (move this up).
```
      viaForeignKeyFields:
        - compound_id
```
            
TODO: Show how to add a general record condition.
```
      recordCondition:
        sql: 'not $$.id = 2'
```

