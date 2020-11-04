$JAR="$PSScriptRoot/../target/dagen.jar"

Write-Host "Generating query SQL and matching Java types..."
java -cp "$JAR" `
  org.sqljson.QueryGeneratorMain `
    --types-language:Java `
    --types-file-header:$PSScriptRoot/types-file-imports `
    $PSScriptRoot/db/dbmd-ora.yaml `
    $PSScriptRoot/query-specs-java.yaml `
    $PSScriptRoot/output/ora `
    $PSScriptRoot/output/ora
Write-Host "  Done"
