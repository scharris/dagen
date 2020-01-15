package org.sqljson;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import static java.util.stream.Collectors.toSet;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.sqljson.TestsBase.Params.params;

import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.sqljson.dbmd.DatabaseMetadata;
import org.sqljson.specs.queries.QueryGroupSpec;

import generated.query.*;


class QueryGeneratorTests extends TestsBase
{
    final DatabaseMetadata dbmd;

    QueryGeneratorTests() throws IOException
    {
        this.dbmd = getDatabaseMetadata("dbmd-pg.yaml");
    }

    @BeforeAll
    @DisplayName("Check that the testing database is available.")
    static void checkDatabaseConnection()
    {
        assertTestDatabaseAvailable();
    }


    @Test
    @DisplayName("Query for single drug table row in multi-column-rows result mode yields expected column values.")
    void readDrugNativeFields() throws Exception
    {
        String sql = getGeneratedQuerySql("drug fields query with param(multi column rows).sql");

        SqlParameterSource params = params(DrugFieldsQueryWithParam.drugIdParam, 2L);

        doQuery(sql, params, rs -> {
            assertEquals(rs.getLong(1), 2);
            assertEquals(rs.getString(2), "Test Drug 2");
            assertEquals(rs.getString(3), "MESH2");
        });
    }

    @Test
    @DisplayName("Query for single drug table row in multi-column-rows result mode using 'otherCondition' to find the row.")
    void readDrugNativeFieldsViaOtherCondition() throws Exception
    {
        String sql = getGeneratedQuerySql("drug fields query with other cond(multi column rows).sql");

        SqlParameterSource params = params("idMinusOne", 1L);

        doQuery(sql, params, rs -> {
            assertEquals(rs.getLong(1), 2);
            assertEquals(rs.getString(2), "Test Drug 2");
            assertEquals(rs.getString(3), "MESH2");
        });
    }


    @Test
    @DisplayName("Query for one drug selecting a subset of native fields, deserialize result row to generated type.")
    void readDrugNativeFieldsAsGeneratedType() throws Exception
    {
        String sql = getGeneratedQuerySql("drug fields query with param(json object rows).sql");

        SqlParameterSource params = params(DrugFieldsQueryWithParam.drugIdParam, 2L);

        doQuery(sql, params, rs -> {
            DrugFieldsQueryWithParam.Drug res = readJson(rs.getString(1), DrugFieldsQueryWithParam.Drug.class);
            assertEquals(res.id, 2);
            assertEquals(res.name, "Test Drug 2");
            assertEquals(res.meshId, opt("MESH2"));
        });
    }

    @Test
    @DisplayName("Query for a drug selecting some native fields, with a field type customized, deserialize to generated type.")
    void readDrugNativeFieldsWithOneCustomizedAsGeneratedType() throws Exception
    {
        String sql = getGeneratedQuerySql("drug fields customized type query(json object rows).sql");

        SqlParameterSource params = params(DrugFieldsCustomizedTypeQuery.drugIdParam, 2L);

        doQuery(sql, params, rs -> {
            DrugFieldsCustomizedTypeQuery.Drug res = readJson(rs.getString(1), DrugFieldsCustomizedTypeQuery.Drug.class);
            assertEquals(res.id, 2);
            assertTrue((res.cid.get() instanceof Integer));
        });
    }

    @Test
    @DisplayName("Query for a drug selecting some native fields, with a field type customized, deserialize to generated type.")
    void readDrugWithFieldExpression() throws Exception
    {
        String sql = getGeneratedQuerySql("drug with field expression query(json object rows).sql");

        SqlParameterSource params = params(DrugWithFieldExpressionQuery.drugIdParam, 2L);

        doQuery(sql, params, rs -> {
            DrugWithFieldExpressionQuery.Drug res = readJson(rs.getString(1), DrugWithFieldExpressionQuery.Drug.class);
            assertEquals(res.id, 2);
            assertEquals(res.cidPlus1000, opt(198 + 1000));
        });
    }

    @Test
    @DisplayName("Query for a drug with related brands child collection included, deserialize to generated type.")
    void readDrugWithBrandsChildCollection() throws Exception
    {
        String sql = getGeneratedQuerySql("drug with brands query(json object rows).sql");

        SqlParameterSource params = params(DrugWithBrandsQuery.drugIdParam, 2L);

        doQuery(sql, params, rs -> {
            DrugWithBrandsQuery.Drug res = readJson(rs.getString(1), DrugWithBrandsQuery.Drug.class);
            assertEquals(res.id, 2);
            List<DrugWithBrandsQuery.Brand> brands = res.brands;
            assertEquals(brands.size(), 1);
            assertEquals(brands.get(0).brandName, "Brand2(TM)");
            assertEquals(brands.get(0).manufacturerId, opt(3L));
        });
    }

    @Test
    @DisplayName("Query for a drug with related brands and advisories, deserialize to generated type.")
    void readDrugWithBrandsAndAdvisoriesChildCollections() throws Exception
    {
        String sql = getGeneratedQuerySql("drug with brands and advisories query(json object rows).sql");

        SqlParameterSource params = params(DrugWithBrandsAndAdvisoriesQuery.drugIdParam, 2L);

        doQuery(sql, params, rs -> {
            DrugWithBrandsAndAdvisoriesQuery.Drug res = readJson(rs.getString(1), DrugWithBrandsAndAdvisoriesQuery.Drug.class);
            assertEquals(res.id, 2);

            List<DrugWithBrandsAndAdvisoriesQuery.Brand> brands = res.brands;
            assertEquals(brands.size(), 1);
            assertEquals(brands.get(0).brandName, "Brand2(TM)");
            assertEquals(brands.get(0).manufacturerId, opt(3L));

            List<DrugWithBrandsAndAdvisoriesQuery.Advisory> advisories = res.advisories;
            assertEquals(advisories.size(), 3);
            Set<String> expectedAdvisories = new HashSet<>();
            expectedAdvisories.add("Advisory concerning drug 2");
            expectedAdvisories.add("Caution concerning drug 2");
            expectedAdvisories.add("Heard this might be bad -anon2");
            assertEquals(advisories.stream().map(a -> a.advisoryText).collect(toSet()), expectedAdvisories);
        });
    }


    @Test
    @DisplayName("Query for an advisory with inline advisory type parent, deserialize to generated type.")
    void readAdvisoryWithInlineAdvisoryTypeParent() throws Exception
    {
        String sql = getGeneratedQuerySql("advisory with inline advisory type query(json object rows).sql");

        SqlParameterSource params = params(AdvisoryWithInlineAdvisoryTypeQuery.advisoryIdParam, 201L);

        // TODO: Test for field expression "exprYieldingTwo".
        doQuery(sql, params, rs -> {
            AdvisoryWithInlineAdvisoryTypeQuery.Advisory res = readJson(rs.getString(1), AdvisoryWithInlineAdvisoryTypeQuery.Advisory.class);
            assertEquals(res.id, 201);
            assertEquals(res.drugId, 2);
            assertEquals(res.advisoryType, "Boxed Warning");
        });
    }

    @Test
    @DisplayName("Query for a drug with wrapped (object ref) analyst parent, deserialize to generated type.")
    void readDrugWithWrappedAnalystParent() throws Exception
    {
        String sql = getGeneratedQuerySql("drug with wrapped analyst query(json object rows).sql");

        SqlParameterSource params = params(DrugWithWrappedAnalystQuery.drugIdParam, 2L);

        doQuery(sql, params, rs -> {
            DrugWithWrappedAnalystQuery.Drug res = readJson(rs.getString(1), DrugWithWrappedAnalystQuery.Drug.class);
            assertEquals(res.id, 2);
            assertEquals(res.registeredByAnalyst.id, 1);
            assertEquals(res.registeredByAnalyst.shortName, "jdoe");
        });
    }

    @Test
    @DisplayName("Query for a drug with an explicit foreign key reference to compound, deserialize to generated type.")
    void readDrugWithCompoundByExplicitForeignKey() throws Exception
    {
        String sql = getGeneratedQuerySql("drug with explicit compound reference query(json object rows).sql");

        SqlParameterSource params = params(DrugWithExplicitCompoundReferenceQuery.drugIdParam, 2L);

        doQuery(sql, params, rs -> {
            DrugWithExplicitCompoundReferenceQuery.Drug res = readJson(rs.getString(1), DrugWithExplicitCompoundReferenceQuery.Drug.class);
            assertEquals(res.id, 2);
            assertEquals(res.compound.displayName, opt("Test Compound 2"));
        });
    }

    @Test
    void rejectBadForeignKeyReferenceInQuerySpec()
    {
        QueryGroupSpec queryGroupSpec = readBadQuerySpec("bad-foreign-key-field-ref.yaml");
        QueryGenerator queryGenerator =
            new QueryGenerator(
                dbmd,
                queryGroupSpec.getDefaultSchema(),
                new HashSet<>(queryGroupSpec.getGenerateUnqualifiedNamesForSchemas()),
                queryGroupSpec.getOutputFieldNameDefault().toFunctionOfFieldName()
            );
        Throwable t = assertThrows(RuntimeException.class, () -> queryGenerator.generateQueries(queryGroupSpec.getQuerySpecs()));
        String msg = t.getMessage().toLowerCase();
        assertTrue(msg.contains("foreign key not found") && msg.contains("x_compound_id"));
    }

    @Test
    void rejectBadFieldReferenceInQuerySpec()
    {
        QueryGroupSpec queryGroupSpec = readBadQuerySpec("bad-field-ref.yaml");
        QueryGenerator queryGenerator =
            new QueryGenerator(
                dbmd,
                queryGroupSpec.getDefaultSchema(),
                new HashSet<>(queryGroupSpec.getGenerateUnqualifiedNamesForSchemas()),
                queryGroupSpec.getOutputFieldNameDefault().toFunctionOfFieldName()
            );
        Throwable t = assertThrows(RuntimeException.class, () -> queryGenerator.generateQueries(queryGroupSpec.getQuerySpecs()));
        String msg = t.getMessage().toLowerCase();
        assertTrue(msg.contains("no metadata for field drug.xname"));
    }

    @Test
    void rejectBadTableReferenceInQuerySpec()
    {
        QueryGroupSpec queryGroupSpec = readBadQuerySpec("bad-table-ref.yaml");
        QueryGenerator queryGenerator =
            new QueryGenerator(
                dbmd,
                queryGroupSpec.getDefaultSchema(),
                new HashSet<>(queryGroupSpec.getGenerateUnqualifiedNamesForSchemas()),
                queryGroupSpec.getOutputFieldNameDefault().toFunctionOfFieldName()
            );
        Throwable t = assertThrows(RuntimeException.class, () -> queryGenerator.generateQueries(queryGroupSpec.getQuerySpecs()));
        String msg = t.getMessage().toLowerCase();
        assertTrue(msg.contains("table drugs.xdrug not found"));
    }
}
