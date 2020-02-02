#!/bin/sh

cd $(dirname $0)

JAR=../target/dagen-jar-with-dependencies.jar

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
    query-specs.yaml \
    output \
    output
echo "  Done"

echo Generating mod statements...
java -cp "$JAR" \
  org.sqljson.ModStatementGeneratorMain \
  --types-language:Java \
  --package:org.mymods \
  output/dbmd-pg.yaml \
  mod-specs.yaml \
  output/mod-stmts \
  output/mod-stmts
echo "  Done"
