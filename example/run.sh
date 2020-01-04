#!/bin/sh

cd $(dirname $0)

# Generate database metadata.
java -cp target/dml-gen.jar \
  org.sqljson.DatabaseMetadataMain \
  db/dbmd-pg.props \
  db/dbmd-pg.props \
  db/dbmd-pg.yaml

# Generate queries.
java -cp target/dml-gen.jar \
  org.sqljson.QueryGeneratorMain \
    --types-language:Java \
    --java-nullability:optwrapped \
    --types-file-header:types-file-imports \
    db/dbmd-pg.yaml \
    query-specs.yaml \
    output \
    output

# Generate mod statements.
java -cp target/dml-gen.jar \
  org.sqljson.ModStatementGeneratorMain \
  --types-language:Java \
  --package:org.mymods \
  db/dbmd-pg.yaml \
  mod-specs.yaml \
  output/mod-stmts \
  output/mod-stmts

