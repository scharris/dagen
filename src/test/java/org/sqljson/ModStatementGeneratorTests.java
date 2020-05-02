package org.sqljson;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

import org.sqljson.dbmd.DatabaseMetadata;
import static org.sqljson.TestsBase.Params.params;

import generated.mod_stmt.*;


@SuppressWarnings("nullness")
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
      String sql = getGeneratedModStatementSql(DrugInsertNamedParams.sqlResource);

      SqlParameterSource params =
         params(
            DrugInsertNamedParams.idParam, 99L,
            DrugInsertNamedParams.nameParam, "test drug",
            DrugInsertNamedParams.compoundIdParam, 1,
            DrugInsertNamedParams.registeredByParam, 1
         );

      doUpdateWithNamedParams(sql, params, (count, jdbc) -> {
         assertEquals(count, 1);
         Map<String,Object> row = jdbc.queryForMap(
            "select id, name, compound_id, registered_by from drug where id = ?",
            99
         );
         assertEquals(row.get("id"), 99);
         assertEquals(row.get("name"), "test drug");
         assertEquals(row.get("compound_id"), 1);
         assertEquals(row.get("registered_by"), 1);
      });
   }

   @Test
   @DisplayName("Insert drug record using numbered parameters.")
   void insertDrugWithNumberedParams() throws Exception
   {
      String sql = getGeneratedModStatementSql(DrugInsertNumberedParams.sqlResource);

      PreparedStatementSetter pstmtSetter = pstmt -> {
         pstmt.setLong(DrugInsertNumberedParams.idParamNum, 99L);
         pstmt.setString(DrugInsertNumberedParams.nameParamNum, "test drug");
         pstmt.setLong(DrugInsertNumberedParams.compoundIdParamNum, 1L);
         pstmt.setLong(DrugInsertNumberedParams.registeredByParamNum, 1);
      };

      doUpdate(sql, pstmtSetter, (count, jdbc) -> {
         assertEquals(count, 1);
         Map<String,Object> row = jdbc.queryForMap("select id, name, compound_id, registered_by from drug where id = ?", 99);
         assertEquals(row.get("id"), 99);
         assertEquals(row.get("name"), "test drug");
         assertEquals(row.get("compound_id"), 1);
         assertEquals(row.get("registered_by"), 1);
      });
   }

   @Test
   @DisplayName("Insert drug with literal field value expression.")
   void insertDrugWithLiteralFieldValueExpr() throws Exception
   {
      String sql = getGeneratedModStatementSql(DrugInsertWithLiteralFieldValueExpr.sqlResource);

      SqlParameterSource params =
         params(
            DrugInsertWithLiteralFieldValueExpr.idParam, 99L,
            DrugInsertWithLiteralFieldValueExpr.nameParam, "test drug",
            DrugInsertWithLiteralFieldValueExpr.registeredByParam, 1
         );

      doUpdateWithNamedParams(sql, params, (count, jdbc) -> {
         assertEquals(count, 1);
         Map<String,Object> row = jdbc.queryForMap("select id, name, compound_id, registered_by from drug where id = ?", 99);
         assertEquals(row.get("id"), 99);
         assertEquals(row.get("name"), "test drug");
         assertEquals(row.get("compound_id"), 3);
         assertEquals(row.get("registered_by"), 1);
      });
   }

   @Test
   @DisplayName("Insert drug with parameterized field value expression.")
   void insertDrugWithParameterizedFieldValueExpr() throws Exception
   {
      String sql = getGeneratedModStatementSql(DrugInsertWithMultiParamFieldValueExpr.sqlResource);

      SqlParameterSource params =
         params(
            DrugInsertWithMultiParamFieldValueExpr.idParam, 99L,
            DrugInsertWithMultiParamFieldValueExpr.namePrefixParam, "left",
            DrugInsertWithMultiParamFieldValueExpr.nameSuffixParam, "right",
            DrugInsertWithMultiParamFieldValueExpr.compoundIdParam, 1,
            DrugInsertWithMultiParamFieldValueExpr.registeredByParam, 1
         );

      doUpdateWithNamedParams(sql, params, (count, jdbc) -> {
         assertEquals(count, 1);
         Map<String,Object> row = jdbc.queryForMap("select id, name, compound_id, registered_by from drug where id = ?", 99);
         assertEquals(row.get("id"), 99);
         assertEquals(row.get("name"), "left:right");
         assertEquals(row.get("compound_id"), 1);
         assertEquals(row.get("registered_by"), 1);
      });
   }

   @Test
   @DisplayName("Update drug with named parameters.")
   void updateDrugWithNamedParams() throws Exception
   {
      String sql = getGeneratedModStatementSql(DrugUpdateWithNamedParams.sqlResource);

      SqlParameterSource params =
         params(
            DrugUpdateWithNamedParams.idCondParam, 2L,
            DrugUpdateWithNamedParams.nameParam, "new value",
            DrugUpdateWithNamedParams.meshIdParam, "M002"
         );

      doUpdateWithNamedParams(sql, params, (count, jdbc) -> {
         assertEquals(count, 1);
         Map<String,Object> row = jdbc.queryForMap("select id, name, mesh_id from drug where id = ?", 2);
         assertEquals(row.get("id"), 2);
         assertEquals(row.get("name"), "new value");
         assertEquals(row.get("mesh_id"), "M002");
      });
   }

   @Test
   @DisplayName("Update drug with numbered parameters.")
   void updateDrugWithNumberedParams() throws Exception
   {
      String sql = getGeneratedModStatementSql(DrugUpdateWithNumberedParams.sqlResource);

      PreparedStatementSetter pstmtSetter = pstmt -> {
         pstmt.setLong(DrugUpdateWithNumberedParams.idCondParamNum, 2L);
         pstmt.setString(DrugUpdateWithNumberedParams.nameParamNum, "new name");
         pstmt.setString(DrugUpdateWithNumberedParams.meshIdParamNum, "M002");
      };

      doUpdate(sql, pstmtSetter, (count, jdbc) -> {
         assertEquals(count, 1);
         Map<String,Object> row = jdbc.queryForMap("select id, name, mesh_id from drug where id = ?", 2);
         assertEquals(row.get("id"), 2);
         assertEquals(row.get("name"), "new name");
         assertEquals(row.get("mesh_id"), "M002");
      });
   }

   @Test
   @DisplayName("Update drug with numbered parameters with customized name.")
   void updateDrugWithNumberedParamsWithCustomizedName() throws Exception
   {
      String sql = getGeneratedModStatementSql(DrugUpdateWithNumberedParamsWithCustomizedName.sqlResource);

      PreparedStatementSetter pstmtSetter = pstmt -> {
         pstmt.setLong(DrugUpdateWithNumberedParamsWithCustomizedName.idCondParamNum, 2L);
         pstmt.setString(DrugUpdateWithNumberedParamsWithCustomizedName.customNameParamNum, "new name");
         pstmt.setString(DrugUpdateWithNumberedParamsWithCustomizedName.meshIdParamNum, "M002");
      };

      doUpdate(sql, pstmtSetter, (count, jdbc) -> {
         assertEquals(count, 1);
         Map<String,Object> row = jdbc.queryForMap("select id, name, mesh_id from drug where id = ?", 2);
         assertEquals(row.get("id"), 2);
         assertEquals(row.get("name"), "new name");
         assertEquals(row.get("mesh_id"), "M002");
      });
   }

   @Test
   @DisplayName("Update drug with multi-parameter field value expression.")
   void updateDrugWithMultiParamFieldValueExpr() throws Exception
   {
      String sql = getGeneratedModStatementSql(DrugUpdateWithMultiParamExpr.sqlResource);

      SqlParameterSource params =
         params(
            DrugUpdateWithMultiParamExpr.idCondParam, 2L,
            DrugUpdateWithMultiParamExpr.namePartOneParam, "left",
            DrugUpdateWithMultiParamExpr.namePartTwoParam, "right"
         );

      doUpdateWithNamedParams(sql, params, (count, jdbc) -> {
         assertEquals(count, 1);
         Map<String,Object> row = jdbc.queryForMap("select id, name from drug where id = ?", 2);
         assertEquals(row.get("id"), 2);
         assertEquals(row.get("name"), "left-right");
      });
   }

   @Test
   @DisplayName("Delete drug with named params.")
   void deleteDrugWithNamedParams() throws Exception
   {
      String sql = getGeneratedModStatementSql(DrugDeleteNamedParams.sqlResource);

      SqlParameterSource params = params(DrugDeleteNamedParams.idCondParam, 2L);

      doUpdateWithNamedParams(sql, params, (count, jdbc) -> {
         assertEquals(count, 1);
         List<Map<String,Object>> res = jdbc.queryForList("select id from drug where id = ?", 2);
         assertEquals(res.size(), 0);
      });
   }

   @Test
   @DisplayName("Delete drug with numbered params.")
   void deleteDrugWithNumberedParams() throws Exception
   {
      String sql = getGeneratedModStatementSql(DrugDeleteNumberedParams.sqlResource);

      PreparedStatementSetter pstmtSetter = pstmt -> {
         pstmt.setLong(DrugDeleteNumberedParams.idCondParamNum, 2L);
      };

      doUpdate(sql, pstmtSetter, (count, jdbc) -> {
         assertEquals(count, 1);
         List<Map<String,Object>> res = jdbc.queryForList("select id from drug where id = ?", 2);
         assertEquals(res.size(), 0);
      });
   }
}
