package org.sqljson;

import java.io.IOException;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import org.springframework.jdbc.core.namedparam.SqlParameterSource;

import org.sqljson.dbmd.DatabaseMetadata;
import static org.sqljson.TestsBase.Params.params;

import generated.mod_stmt.DrugInsert;
import generated.mod_stmt.DrugInsertWithLiteralFieldValueExpression;
import generated.mod_stmt.DrugInsertWithMultiParamFieldValueExpression;


class ModStatementGeneratorTests extends TestsBase
{
    final DatabaseMetadata dbmd;

    ModStatementGeneratorTests() throws IOException
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
    @DisplayName("Insert drug record using named parameters.")
    void insertDrugWithNamedParams() throws Exception
    {
        String sql = getGeneratedModStatementSql(DrugInsert.sqlResource);

        SqlParameterSource params =
            params(
                DrugInsert.idParam, 99L,
                DrugInsert.nameParam, "test drug",
                DrugInsert.compoundIdParam, 1,
                DrugInsert.registeredByParam, 1
            );

        doUpdateWithNamedParams(sql, params, (count, npjdbc) -> {
            assertEquals(count, 1);
            Map<String,Object> row = npjdbc.queryForMap("select id, name, compound_id, registered_by from drug where id = 99", params());
            assertEquals(row.get("id"), 99);
            assertEquals(row.get("name"), "test drug");
            assertEquals(row.get("compound_id"), 1);
            assertEquals(row.get("registered_by"), 1);
        });
    }

    @Test
    @DisplayName("Insert drug with literal field value expression.")
    void insertDrugWithLiteralFieldValueExpression() throws Exception
    {
        String sql = getGeneratedModStatementSql(DrugInsertWithLiteralFieldValueExpression.sqlResource);

        SqlParameterSource params =
            params(
                DrugInsertWithLiteralFieldValueExpression.idParam, 99L,
                DrugInsertWithLiteralFieldValueExpression.nameParam, "test drug",
                DrugInsertWithLiteralFieldValueExpression.registeredByParam, 1
            );

        doUpdateWithNamedParams(sql, params, (count, npjdbc) -> {
            assertEquals(count, 1);
            Map<String,Object> row = npjdbc.queryForMap("select id, name, compound_id, registered_by from drug where id = 99", params());
            assertEquals(row.get("id"), 99);
            assertEquals(row.get("name"), "test drug");
            assertEquals(row.get("compound_id"), 3);
            assertEquals(row.get("registered_by"), 1);
        });
    }

    @Test
    @DisplayName("Insert drug with parameterized field value expression.")
    void insertDrugWithParameterizedFieldValueExpression() throws Exception
    {
        String sql = getGeneratedModStatementSql(DrugInsertWithMultiParamFieldValueExpression.sqlResource);

        SqlParameterSource params =
            params(
                DrugInsertWithMultiParamFieldValueExpression.idParam, 99L,
                DrugInsertWithMultiParamFieldValueExpression.namePrefixParam, "left",
                DrugInsertWithMultiParamFieldValueExpression.nameSuffixParam, "right",
                DrugInsertWithMultiParamFieldValueExpression.compoundIdParam, 1,
                DrugInsertWithMultiParamFieldValueExpression.registeredByParam, 1
            );

        doUpdateWithNamedParams(sql, params, (count, npjdbc) -> {
            assertEquals(count, 1);
            Map<String,Object> row = npjdbc.queryForMap("select id, name, compound_id, registered_by from drug where id = 99", params());
            assertEquals(row.get("id"), 99);
            assertEquals(row.get("name"), "left:right");
            assertEquals(row.get("compound_id"), 1);
            assertEquals(row.get("registered_by"), 1);
        });
    }
}
