package org.sqljson;

import java.io.IOException;
import java.sql.Connection;
import java.util.HashSet;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.sqljson.dbmd.DatabaseMetadata;
import org.sqljson.specs.queries.QueryGroupSpec;
import org.sqljson.specs.queries.ResultsRepr;


class QueryGeneratorTests extends TestsBase
{
    final DatabaseMetadata dbmd;

    QueryGeneratorTests() throws IOException
    {
        this.dbmd = getDatabaseMetadata("dbmd-pg.yaml");
    }

    @Test
    @DisplayName("Query for single drug table row in multi-column-rows mode yields expected column values.")
    void drugId2NativeFieldsMultiColumnRowsQuery() throws Exception
    {
        try ( Connection conn = getConnection() )
        {
            String sql = generateQuerySql("drug-id2-native-fields.yaml", ResultsRepr.MULTI_COLUMN_ROWS);

            doQuery(sql, rs -> {
                rs.next();
                assertEquals(rs.getLong(1), 2);
                assertEquals(rs.getString(2), "Test Drug 2");
                assertEquals(rs.getString(3), "MESH2");
            });
        }
    }

    private QueryGenerator getQueryGenerator(QueryGroupSpec queryGroupSpec)
    {
        return new QueryGenerator(
            dbmd,
            queryGroupSpec.getDefaultSchema(),
            new HashSet<>(queryGroupSpec.getGenerateUnqualifiedNamesForSchemas()),
            queryGroupSpec.getOutputFieldNameDefault().toFunctionOfFieldName()
        );
    }

    private List<GeneratedQuery> generateQueries(String querySpecResourceName) throws IOException
    {
        QueryGroupSpec queryGroupSpec = readQueryGroupSpec(querySpecResourceName);
        QueryGenerator gen = getQueryGenerator(queryGroupSpec);
        return gen.generateQueries(queryGroupSpec.getQuerySpecs());
    }

    private String generateQuerySql(String querySpecResourceName, ResultsRepr resultsRepr) throws IOException
    {
        List<GeneratedQuery> generatedQueries = generateQueries(querySpecResourceName);
        assertEquals(generatedQueries.size(), 1);
        return generatedQueries.get(0).getSql(resultsRepr);
    }
}
