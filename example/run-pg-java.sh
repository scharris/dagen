#!/bin/sh

cd $(dirname $0)

JAR=../target/dagen.jar

echo Generating database metadata...
java -cp "$JAR" \
  org.sqljson.DatabaseMetadataMain \
  db/dbmd-pg.props \
  db/dbmd-pg.props \
  output/dbmd-pg.yaml
echo "  Done"

echo Generating query SQL and matching Java types...
java -cp "$JAR" \
  org.sqljson.QueryGeneratorMain \
    --types-language:Java \
    --java-nullability:optwrapped \
    --types-file-header:types-file-imports \
    output/dbmd-pg.yaml \
    query-specs-java.yaml \
    output/pg/queries/java \
    output/pg/queries/sql
echo "  Done"

echo Generating mod statements...
java -cp "$JAR" \
  org.sqljson.ModStatementGeneratorMain \
  --types-language:Java \
  --package:org.mymods \
  output/dbmd-pg.yaml \
  mod-specs.yaml \
  output/pg/mod-stmts/java \
  output/pg/mod-stmts/sql
echo "  Done"

echo "Writing query specs json schema..."
java -cp "$JAR" org.sqljson.QueryGeneratorMain --print-spec-json-schema > editor-config/query-specs-schema.json
echo "  Done"

echo "Writing mod statement specs json schema..."
java -cp "$JAR" org.sqljson.QueryGeneratorMain --print-spec-json-schema > editor-config/mod-specs-schema.json
echo "  Done"
