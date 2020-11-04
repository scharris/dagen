$JAR="$PSScriptRoot/../target/dagen.jar"

Write-Host Generating database metadata...
java -cp $JAR org.sqljson.DatabaseMetadataMain `
  $PSScriptRoot/db/dbmd-pg.props `
  $PSScriptRoot/db/dbmd-pg.props `
  $PSScriptRoot/output/pg/dbmd-pg.yaml
Write-Host "  Done"

Write-Host "Generating query SQL and matching TypeScript types..."
java -cp "$JAR" `
  org.sqljson.QueryGeneratorMain `
    --types-language:TypeScript `
    --types-file-header:$PSScriptRoot/types-file-imports `
    $PSScriptRoot/output/pg/dbmd-pg.yaml `
    $PSScriptRoot/query-specs-ts.json `
    $PSScriptRoot/output/pg/queries/ts `
    $PSScriptRoot/output/pg/queries/sql
Write-Host "  Done"

Write-Host "Generating relation metadata Typescript module..."
java -cp "$JAR" `
  org.sqljson.DatabaseRelationClassesGeneratorMain `
  --types-language:TypeScript `
  $PSScriptRoot/output/pg/dbmd-pg.yaml `
  $PSScriptRoot/output/pg/relmds/ts
Write-Host "  Done"

