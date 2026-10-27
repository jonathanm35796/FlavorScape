$ErrorActionPreference = 'Stop'
$workspace = Split-Path $PSScriptRoot -Parent
$record = Join-Path $workspace '.tools/local-session.json'
if (-not (Test-Path $record)) { Write-Host 'No recorded local session.'; exit 0 }
$sessions = ConvertFrom-Json -InputObject (Get-Content $record -Raw)
$processes = @(Get-CimInstance Win32_Process)
$serverPorts = @{ UpdateServer = 7304; GameServer = 13302 }
$stoppedServers = @{}
$stoppedListeners = @{}
$netstat = & "$env:SystemRoot\System32\netstat.exe" -ano -p tcp

foreach ($session in $sessions) {
    $component = [string]$session.Component
    if (-not $serverPorts.ContainsKey($component)) { continue }
    $port = $serverPorts[$component]
    $stopped = $false
    $foundListener = $false
    foreach ($line in $netstat) {
        $columns = @($line -split '\s+' | Where-Object { $_ })
        if ($columns.Count -lt 5 -or $columns[0] -ne 'TCP' -or $columns[3] -ne 'LISTENING') { continue }
        $endpoint = [regex]::Match($columns[1], ':(\d+)$')
        if (-not $endpoint.Success -or [int]$endpoint.Groups[1].Value -ne $port) { continue }
        $listenerPid = 0
        if (-not [int]::TryParse($columns[4], [ref]$listenerPid)) { continue }
        $listener = $processes | Where-Object { $_.ProcessId -eq $listenerPid } | Select-Object -First 1
        if ($listener -and $listener.Name -eq 'java.exe') { $foundListener = $true }
        if ($listener -and $listener.Name -eq 'java.exe' -and -not $stoppedListeners.ContainsKey($listenerPid)) {
            & taskkill.exe /PID $listenerPid /T /F | Out-Null
            if ($LASTEXITCODE -eq 0) {
                $stoppedListeners[$listenerPid] = $true
                $stopped = $true
            } else {
                Write-Warning "Could not stop listener PID $listenerPid on port $port."
            }
        }
    }
    if ($stopped) {
        $stoppedServers[$component] = $true
        Write-Host "Stopped $component listener on 127.0.0.1:$port."
    } elseif (-not $foundListener) {
        Write-Host "No Java listener found for $component on port $port."
    } else {
        Write-Warning "Windows denied stopping $component. Right-click Stop Server.bat and choose Run as administrator, then try again."
    }
}

$remaining = @()
foreach ($session in $sessions) {
    $component = [string]$session.Component
    $process = $processes | Where-Object { $_.ProcessId -eq [int]$session.ProcessId } | Select-Object -First 1
    if (-not $process) { continue }
    if ($process.Name -ne 'powershell.exe') { $remaining += $session; continue }

    $isServer = $serverPorts.ContainsKey($component)
    if ($isServer -and -not $stoppedServers.ContainsKey($component)) {
        $remaining += $session
        continue
    }
    if (-not $isServer) {
        if (-not $session.StartedAtUtc) { $remaining += $session; continue }
        $startedAt = [datetime]::Parse($session.StartedAtUtc).ToUniversalTime()
        $createdAt = [System.Management.ManagementDateTimeConverter]::ToDateTime($process.CreationDate).ToUniversalTime()
        if ([Math]::Abs(($createdAt - $startedAt).TotalSeconds) -gt 5) { $remaining += $session; continue }
    }
    & taskkill.exe /PID $session.ProcessId /T /F | Out-Null
    Write-Host "Closed $component console."
}

if ($remaining.Count -eq 0) { Remove-Item $record -Force }
else { $remaining | ConvertTo-Json | Set-Content $record }
