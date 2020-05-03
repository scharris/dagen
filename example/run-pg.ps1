$JAR="$PSScriptRoot/../target/dagen-jar-with-dependencies.jar"

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
    $PSScriptRoot/query-specs.yaml `
    $PSScriptRoot/output/pg `
    $PSScriptRoot/output/pg
Write-Host "  Done"

Write-Host "Generating mod statements..."
java -cp "$JAR" `
  org.sqljson.ModStatementGeneratorMain `
  --types-language:Java `
  --package:org.mymods `
  $PSScriptRoot/output/pg/dbmd-pg.yaml `
  $PSScriptRoot/mod-specs.yaml `
  $PSScriptRoot/output/pg/mod-stmts `
  $PSScriptRoot/output/pg/mod-stmts
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

