$ErrorActionPreference = 'Stop'
$workspace = Split-Path $PSScriptRoot -Parent
$record = Join-Path $workspace '.tools/local-session.json'
if (-not (Test-Path $record)) { Write-Host 'No recorded local session.'; exit 0 }
$launcher = Join-Path $PSScriptRoot 'run-local.ps1'
foreach ($session in (Get-Content $record -Raw | ConvertFrom-Json)) {
    $process = Get-CimInstance Win32_Process -Filter "ProcessId=$($session.ProcessId)"
    if ($process -and $process.Name -eq 'powershell.exe' -and $process.CommandLine.Contains($launcher)) {
        & taskkill.exe /PID $session.ProcessId /T /F | Out-Null
        Write-Host "Stopped $($session.Component)."
    }
}
