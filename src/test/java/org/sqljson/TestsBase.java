package org.sqljson;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.Optional;
import javax.sql.DataSource;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import org.apache.commons.io.IOUtils;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.sqljson.dbmd.DatabaseMetadata;
import org.sqljson.specs.queries.QueryGroupSpec;


public class TestsBase
{
    protected final ObjectMapper yamlMapper;
    protected final ObjectMapper jsonMapper;

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

    String getGeneratedModStatementSql(String resourceName) throws IOException
    {
        try ( InputStream is = getResourceStream("generated/mod-stmt-sql/" + resourceName) )
        {
            return IOUtils.toString(is, StandardCharsets.UTF_8);
        }
    }

    InputStream getResourceStream(String resource)
    {
        return TestsBase.class.getClassLoader().getResourceAsStream(resource);
    }

    QueryGroupSpec readBadQuerySpec(String name)
    {
        try
        {
            InputStream qSpecInputStream = getResourceStream("bad-query-specs/" + name);
            return yamlMapper.readValue(qSpecInputStream, QueryGroupSpec.class);
        }
        catch(IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    static Connection getTestDatabaseConnection() throws SQLException
    {
        return makeTestDatabaseDataSource().getConnection();
    }

    static SingleConnectionDataSource makeTestDatabaseDataSource()
    {
        try
        {
            String url = "jdbc:postgresql://localhost/drugs";
            SingleConnectionDataSource ds = new SingleConnectionDataSource(url, "drugs", "drugs", false);
            ds.getConnection().setAutoCommit(false);
            return ds;
        }
        catch(SQLException e)
        {
            throw new RuntimeException(e);
        }
    }

    void doQuery(String sql, ResultsProcessor resultsProcessor) throws SQLException
    {
        try( Connection conn = getTestDatabaseConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql) )
        {
            resultsProcessor.process(rs);
        }
    }

    void doUpdateWithNamedParams(String sql, SqlParameterSource params, AfterUpdateCallback action)
    {
        NamedParameterJdbcTemplate npjdbc = new NamedParameterJdbcTemplate(makeTestDatabaseDataSource());

        int affectedCount = npjdbc.update(sql, params);

        try
        {
            action.onStatementFinished(affectedCount, npjdbc);
        }
        finally
        {
            if ( affectedCount > 0 )
                npjdbc.getJdbcOperations().execute("rollback");
        }
    }

    static void assertTestDatabaseAvailable()
    {
        try( Connection conn = getTestDatabaseConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("select 1") )
        {
            if ( !rs.next() ) throw new SQLException("Could not connect execute query in test database.");
        }
        catch(SQLException e)
        {
            throw new RuntimeException("Could not connect to test database, has it been started?");
        }
    }

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

    public static class Params extends MapSqlParameterSource
    {
        public static Params params(Object... nameValuePairs)
        {
            if ( nameValuePairs.length % 2 != 0 )
                throw new IllegalArgumentException("Even number of arguments expected for parameter names and values.");

            Params params = new Params();

            final int numPairs = nameValuePairs.length / 2;
            for ( int pairIx = 0; pairIx < numPairs; ++pairIx )
            {
                Object paramName = nameValuePairs[2 * pairIx];
                if ( !(paramName instanceof String) )
                    throw new IllegalArgumentException("Expected string parameter name for pair " + (pairIx + 1) + ".");

                params.addValue((String) paramName, nameValuePairs[2 * pairIx + 1]);
            }

            return params;
        }
    }

    public interface AfterUpdateCallback
    {
        void onStatementFinished(int affectedRowCount, NamedParameterJdbcTemplate npjdbc);
    }
}
