$JAR="$PSScriptRoot/../target/dagen-jar-with-dependencies.jar"

Write-Host Generating database metadata...
java -cp $JAR org.sqljson.DatabaseMetadataMain `
  db/dbmd-pg.props `
  db/dbmd-pg.props `
  output/dbmd-pg.yaml
Write-Host "  Done"

Write-Host "Generating query SQL and matching Java types..."
java -cp "$JAR" `
  org.sqljson.QueryGeneratorMain `
    --types-language:Java `
    --types-file-header:types-file-imports `
    output/dbmd-pg.yaml `
    query-specs.yaml `
    output `
    output
Write-Host "  Done"

Write-Host "Generating mod statements..."
java -cp "$JAR" `
  org.sqljson.ModStatementGeneratorMain `
  --types-language:Java `
  --package:org.mymods `
  output/dbmd-pg.yaml `
  mod-specs.yaml `
  output/mod-stmts `
  output/mod-stmts
Write-Host "  Done"

Write-Host "Writing query specs json schema..."
$qspecsSchema = "editor-config/query-specs-schema.json"
java -cp "$JAR" org.sqljson.QueryGeneratorMain --print-spec-json-schema | Out-File -Encoding ascii $qspecsSchema
(Get-Content $qspecsSchema -Raw).Replace("`r`n","`n") | Set-Content $qspecsSchema  -Force -NoNewline
Write-Host "  Done"

Write-Host "Writing mod statement specs json schema..."
$mspecsSchema = "editor-config/mod-specs-schema.json"
java -cp "$JAR" org.sqljson.ModStatementGeneratorMain --print-spec-json-schema | Out-File -Encoding ascii $mspecsSchema
(Get-Content $mspecsSchema -Raw).Replace("`r`n","`n") | Set-Content $mspecsSchema  -Force -NoNewline
Write-Host "  Done"
