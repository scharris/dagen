package org.sqljsonquery.queryspec.ktdsl

import kotlin.collections.ArrayList

import org.sqljsonquery.queryspec.OutputFieldNameDefault
import org.sqljsonquery.queryspec.ResultsRepr
import org.sqljsonquery.util.Optionals.optn
import org.sqljsonquery.util.StringFuns.lowerCamelCase
import org.sqljsonquery.util.StringFuns.upperCamelCase


data class QueryGroup(
   val defaultSchema: String? = null,
   val outputFieldNameDefault: OutputFieldNameDefault = OutputFieldNameDefault.CAMELCASE,
   val queries: List<Query>
)

data class Query(
   val queryName: String,
   val resultsRepresentations: List<ResultsRepr>,
   val table: Table,
   val generateResultTypes: Boolean = true
)

data class Table(
   val tableName: String,
   val nativeFields: List<Field> = listOf(),
   val inlineParents: List<InlineParent> = listOf(),
   val referencedParents: List<ReferencedParent> = listOf(),
   val childCollections: List<ChildCollection> = listOf(),
   val filter: String? = null
)

data class Field(
   val databaseFieldName: String,
   val outputName: String?
)

data class ChildCollection(
   val childCollectionName: String,
   val childTable: Table,
   val foreignKeyFields: List<String>? = null,
   val filter: String? = null
)

data class InlineParent(
   val parentTable: Table,
   val childForeignKeyFields: List<String>? = null
)

data class ReferencedParent(
   val referenceFieldName: String,
   val parentTable: Table,
   val childForeignKeyFields: List<String>? = null
)

fun fields(vararg names: String): List<Field>
{
   val result = ArrayList<Field>()
   for (name in names)
      result.add(Field(name, null))
   return result
}

fun fieldsCamelCased(vararg names: String): List<Field>
{
    val result = ArrayList<Field>()
    for (name in names)
      result.add(Field(name, lowerCamelCase(name)))
    return result
}

fun fieldsCamelCasedWithPrefix(prefix: String, vararg names: String): List<Field>
{
   val result = ArrayList<Field>()
   for (name in names)
      result.add(Field(name, prefix + upperCamelCase(name)))
   return result
}

fun QueryGroup.toSpec(): org.sqljsonquery.queryspec.QueryGroupSpec =
   org.sqljsonquery.queryspec.QueryGroupSpec(
      optn(defaultSchema),
      outputFieldNameDefault,
      queries.map { it.toSpec() }
   )

fun Query.toSpec(): org.sqljsonquery.queryspec.QuerySpec =
   org.sqljsonquery.queryspec.QuerySpec(queryName, resultsRepresentations, generateResultTypes, table.toSpec())

fun InlineParent.toSpec(): org.sqljsonquery.queryspec.InlineParentSpec =
   org.sqljsonquery.queryspec.InlineParentSpec(parentTable.toSpec(), optn(childForeignKeyFields))

fun ReferencedParent.toSpec(): org.sqljsonquery.queryspec.ReferencedParentSpec =
   org.sqljsonquery.queryspec.ReferencedParentSpec(referenceFieldName, parentTable.toSpec(), optn(childForeignKeyFields))

fun ChildCollection.toSpec(): org.sqljsonquery.queryspec.ChildCollectionSpec =
   org.sqljsonquery.queryspec.ChildCollectionSpec(
      childCollectionName, childTable.toSpec(), optn(foreignKeyFields), optn(filter)
   )

fun Field.toSpec(): org.sqljsonquery.queryspec.TableOutputField =
   org.sqljsonquery.queryspec.TableOutputField(databaseFieldName, optn(outputName))

fun Table.toSpec(): org.sqljsonquery.queryspec.TableOutputSpec {
   val nativeFields = nativeFields.map { it.toSpec() }
   val inlineParents = inlineParents.map { it.toSpec() }
   val referencedParents = referencedParents.map { it.toSpec() }
   val childTables = childCollections.map { it.toSpec() }

   return org.sqljsonquery.queryspec.TableOutputSpec(
      tableName,
      nativeFields,
      inlineParents,
      referencedParents,
      childTables,
      optn(filter)
   )
}

