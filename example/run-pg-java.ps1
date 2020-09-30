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

Write-Host "Generating mod statements and companion Java types..."
java -cp "$JAR" `
  org.sqljson.ModStatementGeneratorMain `
  --types-language:Java `
  --package:org.mymods `
  $PSScriptRoot/output/pg/dbmd-pg.yaml `
  $PSScriptRoot/mod-specs.yaml `
  $PSScriptRoot/output/pg/mod-stmts/java `
  $PSScriptRoot/output/pg/mod-stmts/sql
Write-Host "  Done"

Write-Host "Generating relation metadata Java types..."
java -cp "$JAR" `
  org.sqljson.DatabaseRelationClassesGeneratorMain `
  --types-language:Java `
  --package:org.relmds `
  $PSScriptRoot/output/pg/dbmd-pg.yaml `
  $PSScriptRoot/output/pg/relmds/java
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

