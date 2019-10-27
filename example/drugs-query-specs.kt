import org.sqljsonquery.queryspec.ResultsRepr
import org.sqljsonquery.queryspec.ktdsl.*


val drugsQuery: Query =
   Query("drugs query",
      listOf(ResultsRepr.JSON_OBJECT_ROWS, ResultsRepr.JSON_ARRAY_ROW),
      Table("drug",
         fieldsCamelCased("name", "mesh_id", "cid", "therapeutic_indications"),
         childCollections = listOf(
            ChildCollection("references",
               childTable = Table("drug_reference",
                  inlineParents = listOf(
                     InlineParent(
                        Table("reference",
                           fieldsCamelCased("publication")
                        )
                     )
                  )
               )
            ),
            ChildCollection("brands",
               childTable = Table("brand",
                  fieldsCamelCased("brand_name"),
                  inlineParents = listOf(
                     InlineParent(
                        Table("manufacturer",
                           listOf(Field("name", "manufacturer"))
                        )
                     )
                  )
               )
            ),
            ChildCollection("advisories",
               childTable = Table("advisory",
                  listOf(Field("text", "advisoryText")),
                  inlineParents = listOf(
                     InlineParent(
                        Table("authority",
                           fieldsCamelCasedWithPrefix("authority", "name", "url", "description")
                        )
                     )
                  )
               )
            ),
            ChildCollection("functionalCategories",
               childTable = Table("drug_functional_category",
                  inlineParents = listOf(
                     InlineParent(
                        Table("functional_category",
                           fieldsCamelCasedWithPrefix("category", "name", "description")
                        )
                     ),
                     InlineParent(
                        Table("authority",
                           fieldsCamelCasedWithPrefix("authority", "name", "url", "description")
                        )
                     )
                  )
               )
            )
         ),
         referencedParents = listOf(
            ReferencedParent("compound",
               parentTable = Table("compound",
                  fieldsCamelCased("display_name", "nctr_isis_id", "cas")
               )
            )
         )
      ),
      generateResultTypes = true
   )

val drugsQueryGroup = QueryGroup(defaultSchema = "drugs", queries = listOf(drugsQuery))
