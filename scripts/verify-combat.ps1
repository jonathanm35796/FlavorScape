param()
$ErrorActionPreference = 'Stop'
$workspace = Split-Path $PSScriptRoot -Parent
$env:JAVA_HOME = (Get-ChildItem (Join-Path $workspace '.tools/jdk21') -Directory | Select-Object -First 1).FullName
$env:GRADLE_USER_HOME = Join-Path $workspace '.tools/gradle-home'
Push-Location (Join-Path $workspace 'main')
try {
    # Windows PowerShell otherwise reports ordinary compiler stderr warnings as native errors.
    $ErrorActionPreference = 'Continue'
    & .\gradlew.bat ':llyeta-server:test' ':llyeta-server:combatItemAudit' '--console=plain' '--no-daemon' 2>&1 | ForEach-Object { $_.ToString() }
    $buildExitCode = $LASTEXITCODE
    $ErrorActionPreference = 'Stop'
    if ($buildExitCode -ne 0) { throw "Combat verification failed ($buildExitCode)." }
} finally { Pop-Location }
exit 0
