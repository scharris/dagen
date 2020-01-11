package org.sqljson;

import java.io.IOException;

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
                DrugInsert.meshIdParam, "MESH99",
                DrugInsert.registeredByParam, 1
            );

        doUpdateWithNamedParams(sql, params, count -> {
            assertEquals(count, 1);
        });
    }

    @Test
    @DisplayName("Insert drug with literal field value expression.")
    void insertDrugWithLiteralFieldValueExpression() throws Exception
    {
        String sql = getGeneratedModStatementSql(DrugInsertWithLiteralFieldValueExpression.sqlResource);

        SqlParameterSource params = params(DrugInsertWithLiteralFieldValueExpression.idParam, 99L);

        doUpdateWithNamedParams(sql, params, count -> {
            assertEquals(count, 1);
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
                DrugInsertWithMultiParamFieldValueExpression.nameSuffixParam, "right"
            );

        System.out.println(params);

        doUpdateWithNamedParams(sql, params, count -> {
            assertEquals(count, 1);
        });
    }
}
