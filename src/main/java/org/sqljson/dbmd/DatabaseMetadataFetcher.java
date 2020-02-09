package org.sqljson.dbmd;

import java.sql.*;
import java.util.*;
import java.util.regex.Pattern;
import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

import org.checkerframework.checker.nullness.qual.Nullable;

import org.sqljson.util.StringFuns;
import org.sqljson.dbmd.RelMetadata.RelType;
import static org.sqljson.dbmd.RelMetadata.RelType.Table;
import static org.sqljson.dbmd.RelMetadata.RelType.View;


public class DatabaseMetadataFetcher
{

    public enum DateMapping { DATES_AS_DRIVER_REPORTED, DATES_AS_TIMESTAMPS, DATES_AS_DATES }

    private DateMapping dateMapping;

    public DatabaseMetadataFetcher()
    {
        this(DateMapping.DATES_AS_DRIVER_REPORTED);
    }

    public DatabaseMetadataFetcher(DateMapping mapping)
    {
        this.dateMapping = mapping;
    }

    public void setDateMapping(DateMapping mapping)
    {
        this.dateMapping = mapping;
    }

    public DatabaseMetadata fetchMetadata
    (
        Connection conn,
        @Nullable String schema,
        boolean includeTables,
        boolean includeViews,
        boolean includeFks,
        @Nullable Pattern excludeRelsPattern
    )
        throws SQLException
    {
        return
            fetchMetadata(
                conn.getMetaData(),
                schema,
                includeTables,
                includeViews,
                includeFks,
                excludeRelsPattern
            );
    }

    public DatabaseMetadata fetchMetadata
    (
        DatabaseMetaData dbmd,
        @Nullable String schema,
        boolean includeTables,
        boolean includeViews,
        boolean includeFks,
        @Nullable Pattern excludeRelsPat
    )
        throws SQLException
    {
        CaseSensitivity caseSens = getDatabaseCaseSensitivity(dbmd);

        @Nullable String nSchema = schema != null ? normalizeDatabaseIdentifier(schema, caseSens) : null;

        List<RelDescr> relDescrs = fetchRelationDescriptions(dbmd, nSchema, includeTables, includeViews, excludeRelsPat);

        List<RelMetadata> relMds = fetchRelationMetadatas(relDescrs, nSchema, dbmd);

        List<ForeignKey> fks = includeFks ? fetchForeignKeys(nSchema, dbmd, excludeRelsPat) : emptyList();

        String dbmsName = dbmd.getDatabaseProductName();
        String dbmsVer = dbmd.getDatabaseProductVersion();
        int dbmsMajorVer = dbmd.getDatabaseMajorVersion();
        int dbmsMinorVer = dbmd.getDatabaseMinorVersion();

        return
            new DatabaseMetadata(
                nSchema,
                relMds,
                fks,
                caseSens,
                dbmsName,
                dbmsVer,
                dbmsMajorVer,
                dbmsMinorVer
            );
    }


    public List<RelDescr> fetchRelationDescriptions
    (
        Connection conn,
        String schema,
        boolean includeTables,
        boolean includeViews,
        @Nullable Pattern excludeRelsPattern
    )
        throws SQLException
    {
        return
            fetchRelationDescriptions(
                conn.getMetaData(),
                schema,
                includeTables,
                includeViews,
                excludeRelsPattern
            );
    }

    public List<RelDescr> fetchRelationDescriptions
    (
        DatabaseMetaData dbmd,
        @Nullable String schema,
        boolean includeTables,
        boolean includeViews,
        @Nullable Pattern excludeRelsPattern
    )
        throws SQLException
    {
        List<RelDescr> relDescrs = new ArrayList<>();

        Set<String> relTypes = new HashSet<>();
        if ( includeTables )
            relTypes.add("TABLE");
        if ( includeViews )
            relTypes.add("VIEW");

        ResultSet rs = dbmd.getTables(null, schema, null, relTypes.toArray(new String[0]));

        while ( rs.next() )
        {
            @Nullable String relSchema = rs.getString("TABLE_SCHEM");
            String relName = requireNonNull(rs.getString("TABLE_NAME"));

            RelId relId = new RelId(relSchema, relName);

            if ( !StringFuns.matches(excludeRelsPattern, relId.getIdString()) )
            {
                RelType relType = requireNonNull(rs.getString("TABLE_TYPE")).toLowerCase().equals("table") ? Table : View;

                relDescrs.add(new RelDescr(relId, relType, rs.getString("REMARKS")));
            }
        }

        return relDescrs;
    }

    public List<RelMetadata> fetchRelationMetadatas
    (
        List<RelDescr> relDescrs, // descriptions of relations to include
        @Nullable String schema,
        Connection conn
    )
        throws SQLException
    {
        return fetchRelationMetadatas(relDescrs, schema, conn.getMetaData());
    }

    public List<RelMetadata> fetchRelationMetadatas
    (
        List<RelDescr> relDescrs, // descriptions of relations to include
        @Nullable String schema,
        DatabaseMetaData dbmd
    )
        throws SQLException
    {
        Map<RelId,RelDescr> relDescrsByRelId = relDescrs.stream().collect(toMap(RelDescr::getRelationId, identity()));

        try ( ResultSet colsRS = dbmd.getColumns(null, schema, "%", "%") )
        {
            List<RelMetadata> relMds = new ArrayList<>();
            @Nullable RelMetadataBuilder rmdBldr = null;

            while ( colsRS.next() )
            {
                @Nullable String relSchema = colsRS.getString("TABLE_SCHEM");
                String relName = requireNonNull(colsRS.getString("TABLE_NAME"));

                RelId relId = new RelId(relSchema, relName);

                @Nullable RelDescr relDescr = relDescrsByRelId.get(relId);
                if ( relDescr != null ) // Include this relation?
                {
                    Field f = makeField(colsRS, dbmd);

                    // Relation changed ?
                    if ( rmdBldr == null || !relId.equals(rmdBldr.getRelId()) )
                    {
                        // finalize previous if any
                        if ( rmdBldr != null )
                            relMds.add(rmdBldr.build());

                        rmdBldr = new RelMetadataBuilder(relId, relDescr.getRelationType(), relDescr.getRelationComment());
                    }

                    rmdBldr.addField(f);
                }
            }

            if ( rmdBldr != null )
                relMds.add(rmdBldr.build());

            return relMds;
        }
    }

    public List<ForeignKey> fetchForeignKeys
    (
        @Nullable String schema,
        Connection conn,
        @Nullable Pattern excludeRelsPattern
    )
        throws SQLException
    {
        return fetchForeignKeys(schema, conn.getMetaData(), excludeRelsPattern);
    }

    public List<ForeignKey> fetchForeignKeys
    (
        @Nullable String schema,
        DatabaseMetaData dbmd,
        @Nullable Pattern excludeRelsPattern
    )
        throws SQLException
    {
        List<ForeignKey> fks = new ArrayList<>();

        // Ignore warning about table-name (third) arg being null, which ora/pg drivers allow. In the future it may be
        // necessary to fetch tables for the schema first and call for the imported keys for each separately.
        try ( @SuppressWarnings("nullness") ResultSet rs = dbmd.getImportedKeys(null, schema, null) )
        {
            @Nullable ForeignKeyBuilder fkBldr = null;

            while ( rs.next() )
            {
                short compNum = rs.getShort(9);

                if ( compNum == 1 ) // starting new fk
                {
                    // Finalize previous fk if any.
                    if ( fkBldr != null && fkBldr.neitherRelMatches(excludeRelsPattern) )
                        fks.add(fkBldr.build());

                    fkBldr = new ForeignKeyBuilder(
                        new RelId(rs.getString("FKTABLE_SCHEM"),
                                  requireNonNull(rs.getString("FKTABLE_NAME"))),
                        new RelId(rs.getString("PKTABLE_SCHEM"),
                                  requireNonNull(rs.getString("PKTABLE_NAME")))
                    );
                }

                requireNonNull(fkBldr).addComponent(
                    new ForeignKey.Component(
                        requireNonNull(rs.getString("FKCOLUMN_NAME")),
                        requireNonNull(rs.getString("PKCOLUMN_NAME"))
                    )
                );
            }

            if ( fkBldr != null && fkBldr.neitherRelMatches(excludeRelsPattern) )
                fks.add(fkBldr.build());
        }

        return fks;
    }


    public CaseSensitivity getDatabaseCaseSensitivity(Connection conn) throws SQLException
    {
        return getDatabaseCaseSensitivity(conn.getMetaData());
    }

    public CaseSensitivity getDatabaseCaseSensitivity(DatabaseMetaData dbmd) throws SQLException
    {
        if ( dbmd.storesLowerCaseIdentifiers() )
            return CaseSensitivity.INSENSITIVE_STORED_LOWER;
        else if ( dbmd.storesUpperCaseIdentifiers() )
            return CaseSensitivity.INSENSITIVE_STORED_UPPER;
        else if ( dbmd.storesMixedCaseIdentifiers() )
            return CaseSensitivity.INSENSITIVE_STORED_MIXED;
        else
            return CaseSensitivity.SENSITIVE;
    }

    public String normalizeDatabaseIdentifier
    (
       String id,
       CaseSensitivity caseSens
    )
    {
        if ( id.startsWith("\"") && id.endsWith("\"") )
            return id;
        else if ( caseSens == CaseSensitivity.INSENSITIVE_STORED_LOWER )
            return id.toLowerCase();
        else if ( caseSens == CaseSensitivity.INSENSITIVE_STORED_UPPER )
            return id.toUpperCase();
        else
            return id;
    }

    protected static @Nullable Integer getRSInt
    (
        ResultSet rs,
        String colName
    )
       throws SQLException
    {
        int i = rs.getInt(colName);
        return rs.wasNull() ? null : i;
    }

    protected Field makeField
    (
       ResultSet colsRS,
       DatabaseMetaData dbmd
    )
       throws SQLException
    {

        try ( ResultSet pkRS = dbmd.getPrimaryKeys(colsRS.getString(1), colsRS.getString(2), requireNonNull(colsRS.getString(3))) )
        {
            // Fetch the primary key field names and part numbers for this relation
            Map<String, Integer> pkSeqNumsByName = new HashMap<>();
            while (pkRS.next())
                pkSeqNumsByName.put(requireNonNull(pkRS.getString(4)), pkRS.getInt(5));
            pkRS.close();

            String name = requireNonNull(colsRS.getString("COLUMN_NAME"));
            int typeCode = requireNonNull(colsRS.getInt("DATA_TYPE"));
            String dbType = requireNonNull(colsRS.getString("TYPE_NAME"));

            // Handle special cases/conversions for the type code.
            if ( typeCode == Types.DATE || typeCode == Types.TIMESTAMP )
                typeCode = getTypeCodeForDateOrTimestampColumn(typeCode, dbType);
            else if ( "XMLTYPE".equals(dbType)  || "SYS.XMLTYPE".equals(dbType) )
                // Oracle uses proprietary "OPAQUE" code of 2007 as of 11.2, should be Types.SQLXML = 2009.
                typeCode = Types.SQLXML;

            @Nullable Integer size = getRSInt(colsRS, "COLUMN_SIZE");
            @Nullable Integer length = Field.isJdbcTypeChar(typeCode) ? size : null;
            @Nullable Integer nullableCode = getRSInt(colsRS, "NULLABLE");
            @Nullable Boolean nullable =
                nullableCode != null && nullableCode == ResultSetMetaData.columnNullable ? Boolean.TRUE :
                nullableCode != null && nullableCode == ResultSetMetaData.columnNoNulls ? Boolean.FALSE :
                null;
            @Nullable Integer fracDigs = Field.isJdbcTypeNumeric(typeCode) ? getRSInt(colsRS, "DECIMAL_DIGITS") : null;
            @Nullable Integer prec = Field.isJdbcTypeNumeric(typeCode) ? size : null;
            @Nullable Integer pkPart = pkSeqNumsByName.get(name);
            @Nullable String comment = colsRS.getString("REMARKS");

            return new Field(name, typeCode, dbType, length, prec, fracDigs, nullable, pkPart, comment);
        }
    }

    private int getTypeCodeForDateOrTimestampColumn
    (
        int driverReportedTypeCode,
        String dbNativeType
    )
    {
        String dbNativeTypeUc = dbNativeType.toUpperCase();

        if ( "DATE".equals(dbNativeTypeUc) )
        {
            if ( dateMapping == DateMapping.DATES_AS_TIMESTAMPS )
                return Types.TIMESTAMP;
            else if ( dateMapping == DateMapping.DATES_AS_DATES )
                return Types.DATE;
        }

        return driverReportedTypeCode;
    }
}
