package org.sqljson;

import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.util.Properties;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.sqljson.dbmd.DatabaseMetadata;
import org.sqljson.specs.queries.QueryGroupSpec;


public class TestsBase
{
    private ObjectMapper yamlMapper;

    TestsBase()
    {
        this.yamlMapper = new ObjectMapper(new YAMLFactory());
        yamlMapper.registerModule(new Jdk8Module());
    }

    DatabaseMetadata getDatabaseMetadata(String resource) throws IOException
    {
        try ( InputStream dbmdIS = getResourceStream(resource) )
        {
            assertNotNull(dbmdIS);
            return yamlMapper.readValue(dbmdIS, DatabaseMetadata.class);
        }
    }

    InputStream getResourceStream(String resource)
    {
        return TestsBase.class.getClassLoader().getResourceAsStream(resource);
    }

    QueryGroupSpec readQueryGroupSpec(String name) throws IOException
    {
        InputStream qSpecInputStream = getResourceStream("query-specs/" + name);
        return yamlMapper.readValue(qSpecInputStream, QueryGroupSpec.class);
    }

    Connection getConnection() throws SQLException
    {
        String url = "jdbc:postgresql://localhost/drugs";
        Properties props = new Properties();
        props.setProperty("user", "drugs");
        props.setProperty("password","drugs");
        return DriverManager.getConnection(url, props);
    }

    void doQuery(String sql, ResultsProcessor resultsProcessor) throws SQLException
    {
        try( Connection conn = getConnection();
             Statement stmt = conn.createStatement(); )
        {
            resultsProcessor.process(stmt.executeQuery(sql));
        }
    }

    interface ResultsProcessor {
        void process(ResultSet rs) throws SQLException;
    }
}
