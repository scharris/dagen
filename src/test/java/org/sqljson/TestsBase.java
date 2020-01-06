package org.sqljson;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.Optional;
import java.util.Properties;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.apache.commons.io.IOUtils;
import org.sqljson.dbmd.DatabaseMetadata;
import org.sqljson.specs.queries.QueryGroupSpec;


public class TestsBase
{
    private final ObjectMapper yamlMapper;
    private final ObjectMapper jsonMapper;

    TestsBase()
    {
        yamlMapper = new ObjectMapper(new YAMLFactory());
        yamlMapper.registerModule(new Jdk8Module());

        jsonMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
        jsonMapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        jsonMapper.registerModule(new Jdk8Module());
    }

    DatabaseMetadata getDatabaseMetadata(String resource) throws IOException
    {
        try ( InputStream dbmdIS = getResourceStream(resource) )
        {
            assertNotNull(dbmdIS);
            return yamlMapper.readValue(dbmdIS, DatabaseMetadata.class);
        }
    }

    String getGeneratedQuerySql(String resourceName) throws IOException
    {
        try ( InputStream is = getResourceStream("generated/query-sql/" + resourceName) )
        {
            return IOUtils.toString(is, StandardCharsets.UTF_8);
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

    protected ObjectMapper getJsonObjectMapper() { return jsonMapper; }

    protected <T> T readJson(String s, Class<T> c)
    {
        try
        {
            return jsonMapper.readValue(s.getBytes(), c);
        }
        catch(Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    protected <T> Optional<T> opt(T t)
    {
        return Optional.of(t);
    }

    protected <T> Optional<T> optn(T t)
    {
        return Optional.ofNullable(t);
    }

    interface ResultsProcessor {
        void process(ResultSet rs) throws SQLException;
    }
}
