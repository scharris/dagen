$JAR="$PSScriptRoot/../target/dagen-jar-with-dependencies.jar"

Write-Host "Generating query SQL and matching Java types..."
java -cp "$JAR" `
  org.sqljson.QueryGeneratorMain `
    --types-language:Java `
    --types-file-header:$PSScriptRoot/types-file-imports `
    $PSScriptRoot/db/dbmd-ora.yaml `
    $PSScriptRoot/query-specs.yaml `
    $PSScriptRoot/output/ora `
    $PSScriptRoot/output/ora
Write-Host "  Done"

Write-Host "Generating mod statements..."
java -cp "$JAR" `
  org.sqljson.ModStatementGeneratorMain `
  --types-language:Java `
  $PSScriptRoot/db/dbmd-ora.yaml `
  $PSScriptRoot/mod-specs.yaml `
  $PSScriptRoot/output/ora `
  $PSScriptRoot/output/ora
Write-Host "  Done"

