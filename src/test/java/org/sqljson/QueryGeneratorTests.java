package org.sqljson;

import java.io.IOException;
import java.sql.Connection;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.sqljson.dbmd.DatabaseMetadata;

import static generated.query.DrugNativeFieldsQuery.Drug;


class QueryGeneratorTests extends TestsBase
{
    final DatabaseMetadata dbmd;

    QueryGeneratorTests() throws IOException
    {
        this.dbmd = getDatabaseMetadata("dbmd-pg.yaml");
    }

    @Test
    @DisplayName("Query for single drug table row in multi-column-rows result mode yields expected column values.")
    void oneDrugNativeFieldsMultiColumnRowsQuery() throws Exception
    {
        try ( Connection conn = getConnection() )
        {
            String sql = getGeneratedQuerySql("drug native fields query(multi column rows).sql");

            doQuery(sql, rs -> {
                rs.next();
                assertEquals(rs.getLong(1), 2);
                assertEquals(rs.getString(2), "Test Drug 2");
                assertEquals(rs.getString(3), "MESH2");
            });
        }
    }

    @Test
    @DisplayName("Query for one drug selecting a subset of native fields, deserialize result row to generated type.")
    void readOneDrug() throws Exception
    {
        try ( Connection conn = getConnection() )
        {
            String sql = getGeneratedQuerySql("drug native fields query(json object rows).sql");

            doQuery(sql, rs -> {
                rs.next();
                Drug res = readJson(rs.getString(1), Drug.class);
                assertEquals(res.id, 2);
                assertEquals(res.name, "Test Drug 2");
                assertEquals(res.meshId, opt("MESH2"));
            });
        }
    }

    /*
    private List<GeneratedQuery> generateQueries(String querySpecResourceName) throws IOException
    {
        QueryGroupSpec queryGroupSpec = readQueryGroupSpec(querySpecResourceName);
        QueryGenerator queryGenerator =
            new QueryGenerator(
                dbmd,
                queryGroupSpec.getDefaultSchema(),
                new HashSet<>(queryGroupSpec.getGenerateUnqualifiedNamesForSchemas()),
                queryGroupSpec.getOutputFieldNameDefault().toFunctionOfFieldName()
            );
        return queryGenerator.generateQueries(queryGroupSpec.getQuerySpecs());
    }
    */
}
