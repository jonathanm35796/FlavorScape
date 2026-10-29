param()
$ErrorActionPreference = 'Stop'
$workspace = Split-Path $PSScriptRoot -Parent
$env:JAVA_HOME = (Get-ChildItem (Join-Path $workspace '.tools/jdk21') -Directory | Select-Object -First 1).FullName
$env:GRADLE_USER_HOME = Join-Path $workspace '.tools/gradle-home'
Push-Location (Join-Path $workspace 'main')
try {
    & .\gradlew.bat ':lletya-api:test' ':lletya-server:test' '--console=plain' '--no-daemon'
    if ($LASTEXITCODE -ne 0) { throw "Roadmap verification failed ($LASTEXITCODE)." }
} finally { Pop-Location }
