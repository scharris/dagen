#!/bin/sh

cd $(dirname $0)

JAR=../target/dagen.jar

echo Generating database metadata...
java -cp "$JAR" \
  org.sqljson.DatabaseMetadataGeneratorMain \
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
