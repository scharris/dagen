package io.sqljson.dbmd;

import java.sql.*;
import java.util.*;
import java.util.regex.Pattern;
import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.empty;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

import io.sqljson.dbmd.RelMetadata.RelType;
import io.sqljson.util.Optionals;
import io.sqljson.util.StringFuns;
import static io.sqljson.dbmd.RelMetadata.RelType.Table;
import static io.sqljson.dbmd.RelMetadata.RelType.View;


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
        Optional<String> schema,
        boolean includeTables,
        boolean includeViews,
        boolean includeFks,
        Optional<Pattern> excludeRelsPattern
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
        Optional<String> schema,
        boolean includeTables,
        boolean includeViews,
        boolean includeFks,
        Optional<Pattern> excludeRelsPat
    )
        throws SQLException
    {
        CaseSensitivity caseSens = getDatabaseCaseSensitivity(dbmd);

        Optional<String> nSchema = schema.map(s -> normalizeDatabaseIdentifier(s, caseSens));

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
        Optional<String> schema,
        boolean includeTables,
        boolean includeViews,
        Optional<Pattern> excludeRelsPattern
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
        Optional<String> schema,
        boolean includeTables,
        boolean includeViews,
        Optional<Pattern> excludeRelsPattern
    )
        throws SQLException
    {
        List<RelDescr> relDescrs = new ArrayList<>();

        Set<String> relTypes = new HashSet<>();
        if ( includeTables )
            relTypes.add("TABLE");
        if ( includeViews )
            relTypes.add("VIEW");

        ResultSet rs = dbmd.getTables(null, schema.orElse(null), null, relTypes.toArray(new String[0]));

        while ( rs.next() )
        {
            Optional<String> relSchema = Optionals.optn(rs.getString("TABLE_SCHEM"));
            String relName = rs.getString("TABLE_NAME");

            RelId relId = new RelId(relSchema, relName);

            if ( !StringFuns.matches(excludeRelsPattern, relId.getIdString()) )
            {
                RelType relType = rs.getString("TABLE_TYPE").toLowerCase().equals("table") ? Table : View;

                relDescrs.add(new RelDescr(relId, relType, Optionals.optn(rs.getString("REMARKS"))));
            }
        }

        return relDescrs;
    }

    public List<RelMetadata> fetchRelationMetadatas
    (
        List<RelDescr> relDescrs, // descriptions of relations to include
        Optional<String> schema,
        Connection conn
    )
        throws SQLException
    {
        return fetchRelationMetadatas(relDescrs, schema, conn.getMetaData());
    }

    public List<RelMetadata> fetchRelationMetadatas
    (
        List<RelDescr> relDescrs, // descriptions of relations to include
        Optional<String> schema,
        DatabaseMetaData dbmd
    )
        throws SQLException
    {
        Map<RelId,RelDescr> relDescrsByRelId = relDescrs.stream().collect(toMap(RelDescr::getRelationId, identity()));

        try ( ResultSet colsRS = dbmd.getColumns(null, schema.orElse(null), "%", "%") )
        {
            List<RelMetadata> relMds = new ArrayList<>();
            RelMetadataBuilder rmdBldr = null;

            while ( colsRS.next() )
            {
                Optional<String> relSchema = Optionals.optn(colsRS.getString("TABLE_SCHEM"));
                String relName = colsRS.getString("TABLE_NAME");

                RelId relId = new RelId(relSchema, relName);

                RelDescr relDescr = relDescrsByRelId.get(relId);
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
        Optional<String> schema,
        Connection conn,
        Optional<Pattern> excludeRelsPattern
    )
        throws SQLException
    {
        return fetchForeignKeys(schema, conn.getMetaData(), excludeRelsPattern);
    }

    public List<ForeignKey> fetchForeignKeys
    (
        Optional<String> schema,
        DatabaseMetaData dbmd,
        Optional<Pattern> excludeRelsPattern
    )
        throws SQLException
    {
        List<ForeignKey> fks = new ArrayList<>();

        try ( ResultSet rs = dbmd.getImportedKeys(null, schema.orElse(null), null) )
        {
            ForeignKeyBuilder fkBldr = null;

            while ( rs.next() )
            {
                short compNum = rs.getShort(9);

                if ( compNum == 1 ) // starting new fk
                {
                    // Finalize previous fk if any.
                    if ( fkBldr != null && fkBldr.neitherRelMatches(excludeRelsPattern) )
                        fks.add(fkBldr.build());

                    fkBldr = new ForeignKeyBuilder(
                        new RelId(Optionals.optn(rs.getString("FKTABLE_SCHEM")),
                                  rs.getString("FKTABLE_NAME")),
                        new RelId(Optionals.optn(rs.getString("PKTABLE_SCHEM")),
                                  rs.getString("PKTABLE_NAME"))
                    );
                    fkBldr.addComponent(
                        new ForeignKey.Component(rs.getString("FKCOLUMN_NAME"), rs.getString("PKCOLUMN_NAME"))
                    );
                }
                else // adding another fk component
                {
                    requireNonNull(fkBldr); // because we should have seen a component # 1 before entering here
                    fkBldr.addComponent(
                        new ForeignKey.Component(rs.getString("FKCOLUMN_NAME"), rs.getString("PKCOLUMN_NAME"))
                    );
                }
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

    protected static Optional<Integer> getRSInt
    (
        ResultSet rs,
        String colName
    )
       throws SQLException
    {
        int i = rs.getInt(colName);
        return rs.wasNull() ? empty() : Optionals.opt(i);
    }

    protected Field makeField
    (
       ResultSet colsRS,
       DatabaseMetaData dbmd
    )
       throws SQLException
    {
        try ( ResultSet pkRS = dbmd.getPrimaryKeys(colsRS.getString(1), colsRS.getString(2), colsRS.getString(3)) )
        {
            // Fetch the primary key field names and part numbers for this relation
            Map<String, Integer> pkSeqNumsByName = new HashMap<>();
            while (pkRS.next())
                pkSeqNumsByName.put(pkRS.getString(4), pkRS.getInt(5));
            pkRS.close();

            String name = colsRS.getString("COLUMN_NAME");
            int typeCode = colsRS.getInt("DATA_TYPE");
            String dbType = colsRS.getString("TYPE_NAME");

            // Handle special cases/conversions for the type code.
            if ( typeCode == Types.DATE || typeCode == Types.TIMESTAMP )
                typeCode = getTypeCodeForDateOrTimestampColumn(typeCode, dbType);
            else if ( "XMLTYPE".equals(dbType)  || "SYS.XMLTYPE".equals(dbType) )
                // Oracle uses proprietary "OPAQUE" code of 2007 as of 11.2, should be Types.SQLXML = 2009.
                typeCode = Types.SQLXML;

            Optional<Integer> size = getRSInt(colsRS, "COLUMN_SIZE");
            Optional<Integer> length = Field.isJdbcTypeChar(typeCode) ? size : empty();
            Optional<Boolean> nullable = getRSInt(colsRS, "NULLABLE").flatMap(n ->
                n == ResultSetMetaData.columnNullable ? Optionals.opt(true) :
                n == ResultSetMetaData.columnNoNulls ? Optionals.opt(false) :
                    empty()
            );
            Optional<Integer> fracDigs =
                Field.isJdbcTypeNumeric(typeCode) ? getRSInt(colsRS, "DECIMAL_DIGITS") : empty();
            Optional<Integer> prec = Field.isJdbcTypeNumeric(typeCode) ? size : empty();
            Optional<Integer> pkPart = Optionals.optn(pkSeqNumsByName.get(name));
            Optional<String> comment = Optionals.optn(colsRS.getString("REMARKS"));

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
