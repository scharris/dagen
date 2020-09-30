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
    $PSScriptRoot/query-specs-ts.yaml `
    $PSScriptRoot/output/pg/queries/ts `
    $PSScriptRoot/output/pg/queries/sql
Write-Host "  Done"

Write-Host "Generating mod statements and companion TypeScript modules..."
java -cp "$JAR" `
  org.sqljson.ModStatementGeneratorMain `
  --types-language:TypeScript `
  --package:org.mymods `
  $PSScriptRoot/output/pg/dbmd-pg.yaml `
  $PSScriptRoot/mod-specs.yaml `
  $PSScriptRoot/output/pg/mod-stmts/ts `
  $PSScriptRoot/output/pg/mod-stmts/sql
Write-Host "  Done"

Write-Host "Generating relation metadata Typescript module..."
java -cp "$JAR" `
  org.sqljson.DatabaseRelationClassesGeneratorMain `
  --types-language:TypeScript `
  $PSScriptRoot/output/pg/dbmd-pg.yaml `
  $PSScriptRoot/output/pg/relmds/ts
Write-Host "  Done"

Write-Host "Writing query specs json schema..."
$qspecsSchema = "$PSScriptRoot/editor-config/query-specs-schema.json"
java -cp "$JAR" org.sqljson.QueryGeneratorMain --print-spec-json-schema | Out-File -Encoding ascii $qspecsSchema
(Get-Content $qspecsSchema -Raw).Replace("`r`n","`n") | Set-Content $qspecsSchema  -Force -NoNewline
Write-Host "  Done"

Write-Host "Writing mod statement specs json schema..."
$mspecsSchema = "$PSScriptRoot/editor-config/mod-specs-schema.json"
java -cp "$JAR" org.sqljson.ModStatementGeneratorMain --print-spec-json-schema | Out-File -Encoding ascii $mspecsSchema
(Get-Content $mspecsSchema -Raw).Replace("`r`n","`n") | Set-Content $mspecsSchema  -Force -NoNewline
Write-Host "  Done"
