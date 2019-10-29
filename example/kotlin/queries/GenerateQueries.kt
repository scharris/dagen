package queries

import java.io.OutputStream
import java.nio.file.Files

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module

import org.sqljsonquery.DatabaseMetadataMain
import org.sqljsonquery.QueryGeneratorMain
import org.sqljsonquery.queryspec.QueryGroupSpec


fun main(args: Array<String>)
{
   val queryGroupYamlPath = Files.createTempFile("query-group", "")

   writeQueryGroupSpecYaml(drugsQueryGroupSpec, Files.newOutputStream(queryGroupYamlPath))

   if ( args.contains("--generate-dbmd") )
      DatabaseMetadataMain.execCommandLine(arrayOf("dbmd.props", "dbmd.yaml"))

   /* TODO:
       - What's wrong with the query spec generated from Kotlin?  It complains about no fk from drug_reference to drug.
    */

   QueryGeneratorMain.execCommandLine(arrayOf(
      "-papp.drugs.queries",
      "-ffield-type-overrides",
      "-lJava",        // TODO: Just use Kotlin output language instead once supported.
      "-noptwrapped",  // "
      "dbmd.yaml",
      "query-specs.yaml", // TODO: queryGroupYamlPath.toString(),
      "target",
      "target"
   ))

   Files.delete(queryGroupYamlPath)
}


fun writeQueryGroupSpecYaml(queryGroupSpec: QueryGroupSpec, os: OutputStream)
{
   val mapper = ObjectMapper(YAMLFactory())
   mapper.registerModule(Jdk8Module())
   mapper.writeValue(os, queryGroupSpec)
}


