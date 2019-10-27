package org.sqljsonquery.queryspec.ktdsl

import org.sqljsonquery.queryspec.ChildCollectionSpec
import org.sqljsonquery.queryspec.InlineParentSpec
import org.sqljsonquery.queryspec.ResultsRepr
import org.sqljsonquery.queryspec.ReferencedParentSpec
import org.sqljsonquery.util.Optionals.optn
import org.sqljsonquery.util.StringFuns.lowerCamelCase
import org.sqljsonquery.util.StringFuns.upperCamelCase

data class QueryGroup(
   val defaultSchema: String? = null,
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

fun toSpec(queryGroup: QueryGroup): org.sqljsonquery.queryspec.QueryGroupSpec =
   org.sqljsonquery.queryspec.QueryGroupSpec(optn(queryGroup.defaultSchema), queryGroup.queries.map { toSpec(it) })

fun toSpec(q: Query): org.sqljsonquery.queryspec.QuerySpec =
   org.sqljsonquery.queryspec.QuerySpec(q.queryName, q.resultsRepresentations, q.generateResultTypes, toSpec(q.table))

fun toSpec(ip: InlineParent): InlineParentSpec =
   InlineParentSpec(toSpec(ip.parentTable), optn(ip.childForeignKeyFields))

fun toSpec(wp: ReferencedParent): ReferencedParentSpec =
   ReferencedParentSpec(wp.referenceFieldName, toSpec(wp.parentTable), optn(wp.childForeignKeyFields))

fun toSpec(cc: ChildCollection): ChildCollectionSpec =
   ChildCollectionSpec(cc.childCollectionName, toSpec(cc.childTable), optn(cc.foreignKeyFields), optn(cc.filter))

fun toSpec(f: Field): org.sqljsonquery.queryspec.TableOutputField =
   org.sqljsonquery.queryspec.TableOutputField(f.databaseFieldName, optn(f.outputName))

fun toSpec(t: Table): org.sqljsonquery.queryspec.TableOutputSpec {
   val nativeFields = t.nativeFields.map { toSpec(it) }
   val inlineParents = t.inlineParents.map { toSpec(it) }
   val referencedParents = t.referencedParents.map { toSpec(it) }
   val childTables = t.childCollections.map { toSpec(it) }

   return org.sqljsonquery.queryspec.TableOutputSpec(
      t.tableName,
      nativeFields,
      inlineParents,
      referencedParents,
      childTables,
      optn(t.filter)
   )
}
