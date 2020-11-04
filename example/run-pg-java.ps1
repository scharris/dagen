$JAR="$PSScriptRoot/../target/dagen.jar"

Write-Host Generating database metadata...
java -cp $JAR org.sqljson.DatabaseMetadataMain `
  $PSScriptRoot/db/dbmd-pg.props `
  $PSScriptRoot/db/dbmd-pg.props `
  $PSScriptRoot/output/pg/dbmd-pg.yaml
Write-Host "  Done"

Write-Host "Generating query SQL and matching Java types..."
java -cp "$JAR" `
  org.sqljson.QueryGeneratorMain `
    --types-language:Java `
    --types-file-header:$PSScriptRoot/types-file-imports `
    $PSScriptRoot/output/pg/dbmd-pg.yaml `
    $PSScriptRoot/query-specs-java.yaml `
    $PSScriptRoot/output/pg/queries/java `
    $PSScriptRoot/output/pg/queries/sql
Write-Host "  Done"

Write-Host "Generating relation metadata Java types..."
java -cp "$JAR" `
  org.sqljson.DatabaseRelationClassesGeneratorMain `
  --types-language:Java `
  --package:org.relmds `
  $PSScriptRoot/output/pg/dbmd-pg.yaml `
  $PSScriptRoot/output/pg/relmds/java
Write-Host "  Done"
