package org.dmlgen;

import java.io.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Optional;
import java.util.Properties;
import java.util.regex.Pattern;
import static java.util.Optional.empty;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;

import static org.dmlgen.util.Files.outputStream;
import static org.dmlgen.util.Optionals.opt;
import static org.dmlgen.util.Optionals.optn;
import static org.dmlgen.util.Props.getProperty;
import static org.dmlgen.util.Props.requireProperty;
import org.dmlgen.dbmd.DatabaseMetadata;
import org.dmlgen.dbmd.DatabaseMetadataFetcher;


public class DatabaseMetadataMain
{
   public static void printUsage(PrintStream ps)
   {
      ps.println("Expected arguments: jdbc-properties-file [dbmd-properties-file] output-file|-");

      ps.println(
         "jdbc properties file properties:\n  " +
            "  jdbc-driver-class\n" +
            "  jdbc-connect-url\n" +
            "  user\n" +
            "  password\n"
      );

      ps.println(
         "dbmd properties file properties:\n  " +
            "  date-mapping (DATES_AS_DRIVER_REPORTED | DATES_AS_TIMESTAMPS | DATES_AS_DATES)\n" +
            "  relations-owner (schema name | *any-owners*)\n" +
            "  exclude-relations-fqname-regex\n"
      );
   }

   public static void main(String[] args)
   {
      try
      {
         execCommandLine(args);
      }
      catch(Exception e)
      {
         System.err.println(e.getMessage());
         System.exit(1);
      }
   }

   public static void execCommandLine(String[] args) throws Exception
   {
      if ( args.length == 1 && args[0].equals("-h") || args[0].equals("--help") )
      {
         printUsage(System.out);
         return;
      }
      else if ( args.length < 2 )
      {
         throw new RuntimeException("Expected 2 or more arguments");
      }

      int argIx = 0;

      String jdbcPropsFilePath = args[argIx++];
      Optional<String> dbmdPropsFilePath = args.length == 3 ? opt(args[argIx++]) : empty();
      String outputFilePath = args[argIx];

      Properties props = new Properties();

      try ( OutputStream os = outputStream(outputFilePath);
            InputStream propsIS = new FileInputStream(jdbcPropsFilePath) )
      {
         props.load(propsIS);

         String driverClassname = requireProperty(props, "jdbc-driver-class", "jdbc.driverClassName");
         String connStr = requireProperty(props, "jdbc-connect-url", "jdbc.url");
         String user = requireProperty(props, "user", "jdbc.username");
         String password = requireProperty(props, "password", "jdbc.password");

         Class.forName(driverClassname);

         try ( Connection conn = DriverManager.getConnection(connStr, user, password) )
         {
            dbmdPropsFilePath.ifPresent(dbmdPropsPath -> {
               try
               {
                  if (!jdbcPropsFilePath.equals(dbmdPropsPath))
                     props.load(new FileInputStream(dbmdPropsPath));
               }
               catch (IOException ioe) { throw new RuntimeException(ioe); }
            });

            Optional<String> dateMappingStr = getProperty(props, "date-mapping");
            DatabaseMetadataFetcher.DateMapping dateMapping =
               dateMappingStr.map(DatabaseMetadataFetcher.DateMapping::valueOf)
               .orElse(DatabaseMetadataFetcher.DateMapping.DATES_AS_DRIVER_REPORTED);

            Optional<String> relsOwner = getProperty(props, "relations-owner").flatMap(o ->
               o.equals("*any-owners*") ? empty() : opt(o)
            );

            Optional<Pattern> excludeRelsPat =
               getProperty(props, "exclude-relations-fqname-regex").map(Pattern::compile);

            DatabaseMetadata dbmd =
               new DatabaseMetadataFetcher(dateMapping)
               .fetchMetadata(
                  conn.getMetaData(),
                  relsOwner,
                  true,
                  true,
                  true,
                  excludeRelsPat
               );

            String outputFormat = optn(props.getProperty("output-format")).orElse("json");

            switch ( outputFormat )
            {
               case "json":
               {
                  ObjectMapper mapper = new ObjectMapper();
                  mapper.registerModule(new Jdk8Module());
                  mapper.enable(SerializationFeature.INDENT_OUTPUT);
                  mapper.writeValue(os, dbmd);
                  break;
               }
               case "yaml":
               {
                  ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
                  mapper.registerModule(new Jdk8Module());
                  mapper.writeValue(os, dbmd);
                  break;
               }
               default:
                  throw new RuntimeException("output format in property must be json or yaml");
                }
            }
        }
    }
}
