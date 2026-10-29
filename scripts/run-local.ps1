param(
    [ValidateSet('All', 'Servers', 'UpdateServer', 'GameServer', 'Client')]
    [string]$Component = 'All',
    [switch]$BuildOnly,
    [switch]$VerifyLogin,
    [switch]$Visible
)

$ErrorActionPreference = 'Stop'
$workspace = Split-Path $PSScriptRoot -Parent
$source = Join-Path $workspace 'main'
$tools = Join-Path $workspace '.tools'
$env:GRADLE_USER_HOME = Join-Path $tools 'gradle-home'
$env:JAVA_HOME = if ($Component -eq 'Client') {
    (Get-ChildItem (Join-Path $tools 'jdk8') -Directory | Select-Object -First 1).FullName
} else {
    (Get-ChildItem (Join-Path $tools 'jdk21') -Directory | Select-Object -First 1).FullName
}
if (-not (Test-Path (Join-Path $env:JAVA_HOME 'bin/java.exe'))) {
    throw 'Required portable JDK is missing. See docs/environment-status.md.'
}

# Runtime state is moved only after the previous server has stopped. Builds and
# source renaming must never copy a live player's save into a second data store.
if (-not $BuildOnly -and $Component -ne 'Client') {
    $legacyModules = @('server', 'update-server')
    foreach ($module in $legacyModules) {
        $legacy = Join-Path $source ('flavor' + 'scape-' + $module)
        $current = Join-Path $source ('llyeta-' + $module)
        $pending = @('server-data', 'server.properties', 'local.properties') | Where-Object {
            Test-Path -LiteralPath (Join-Path $legacy $_)
        }
        if ($pending.Count -eq 0) { continue }
        # netstat does not require the WMI/CIM permissions that can silently make
        # Get-NetTCPConnection appear empty in restricted desktop sessions.
        $netstat = & "$env:SystemRoot/System32/netstat.exe" -ano -p tcp
        if ($LASTEXITCODE -ne 0) { throw 'Cannot verify that game ports are stopped; runtime migration was cancelled.' }
        $listeners = @($netstat | Where-Object {
            $_ -match '^\s*TCP\s+\S+:(7304|13302)\s+\S+\s+LISTENING\s+\d+\s*$'
        })
        if ($listeners) {
            throw 'Stop the existing servers with Stop Server.bat before starting Llyeta; their saves will then migrate automatically.'
        }
        foreach ($entry in $pending) {
            $oldPath = [IO.Path]::GetFullPath((Join-Path $legacy $entry))
            $newPath = [IO.Path]::GetFullPath((Join-Path $current $entry))
            $sourceRoot = [IO.Path]::GetFullPath($source).TrimEnd('\') + '\'
            if (-not $oldPath.StartsWith($sourceRoot, [StringComparison]::OrdinalIgnoreCase) -or
                -not $newPath.StartsWith($sourceRoot, [StringComparison]::OrdinalIgnoreCase)) {
                throw 'Runtime migration path is outside the source directory.'
            }
            if (Test-Path -LiteralPath $newPath) {
                throw "Both old and new runtime state exist for $entry. Resolve the duplicate before starting to avoid overwriting saves."
            }
            Move-Item -LiteralPath $oldPath -Destination $newPath
            if ($entry -eq 'server.properties') {
                $properties = [IO.File]::ReadAllText($newPath)
                $oldName = 'Flavor' + 'Scape'
                $properties = [regex]::Replace($properties, '(?im)^(world_name\s*=\s*)[^\r\n]*', {
                    param($match)
                    $match.Value.Replace($oldName, 'Llyeta').Replace($oldName.ToLowerInvariant(), 'llyeta')
                })
                [IO.File]::WriteAllText($newPath, $properties, (New-Object Text.UTF8Encoding($false)))
            }
        }
    }
}

function Invoke-LocalGradle([string]$Directory, [string[]]$Tasks, [string]$Log) {
    Push-Location $Directory
    try {
        # Windows PowerShell wraps native stderr as errors; Gradle's exit code is authoritative.
        $ErrorActionPreference = 'Continue'
        $stream = [System.IO.File]::Open($Log, 'Create', 'Write', 'ReadWrite')
        $writer = New-Object System.IO.StreamWriter($stream)
        $writer.AutoFlush = $true
        try {
            & .\gradlew.bat @Tasks --console=plain --no-daemon '-Dorg.gradle.warning.mode=summary' 2>&1 | ForEach-Object {
                $line = "$_"
                if ($_ -is [System.Management.Automation.ErrorRecord] -and $line -eq 'System.Management.Automation.RemoteException') { $line = '' }
                $writer.WriteLine($line)
                if ($Visible) { Write-Host $line }
            }
        } finally { $writer.Dispose() }
        $result = $LASTEXITCODE
        if ($result -ne 0) { Get-Content $Log -Tail 40; throw "Gradle failed ($result). See $Log" }
    } finally { Pop-Location }
}

if ($Component -in @('All', 'Servers')) {
    if (-not $BuildOnly) {
        $listeners = Get-NetTCPConnection -State Listen -ErrorAction SilentlyContinue |
            Where-Object { $_.LocalPort -in @(7304, 13302) }
        if ($listeners) { throw 'Local game ports are already in use. Run scripts/stop-local.ps1 first.' }
    }
    Write-Host 'Building game and update servers with JDK 21...'
    Invoke-LocalGradle $source @(':llyeta-server:classes', ':llyeta-update-server:classes') (Join-Path $tools 'main-server-build.log')
    if ($Component -eq 'All') {
        $env:JAVA_HOME = (Get-ChildItem (Join-Path $tools 'jdk8') -Directory | Select-Object -First 1).FullName
        Write-Host 'Building client with JDK 8...'
        Invoke-LocalGradle (Join-Path $source 'runelite') @(':runelite-client:classes') (Join-Path $tools 'main-client-build.log')
    }
    if ($BuildOnly) {
        if ($Component -eq 'All') { Write-Host 'All components compiled successfully.' }
        else { Write-Host 'Server components compiled successfully.' }
        exit 0
    }
    $sessions = @()
    $parts = if ($Component -eq 'All') { @('UpdateServer', 'GameServer', 'Client') } else { @('UpdateServer', 'GameServer') }
    foreach ($part in $parts) {
        if ($part -eq 'Client') {
            Write-Host 'Waiting for localhost game and update listeners...'
            $deadline = (Get-Date).AddSeconds(90)
            do {
                $readyPorts = @(Get-NetTCPConnection -State Listen -ErrorAction SilentlyContinue |
                    Where-Object { $_.LocalAddress -eq '127.0.0.1' -and $_.LocalPort -in @(7304, 13302) } |
                    Select-Object -ExpandProperty LocalPort -Unique)
                if ($readyPorts.Count -eq 2) { break }
                if ((Get-Date) -ge $deadline) { throw 'Server startup timed out. See .tools/main-*Server.log; stop with scripts/stop-local.ps1.' }
                Start-Sleep -Seconds 1
            } while ($true)
        }
        $arguments = '-NoProfile -ExecutionPolicy Bypass -File "{0}" -Component {1}' -f $PSCommandPath, $part
        $windowStyle = 'Hidden'
        if ($Visible) {
            $arguments = '-NoExit ' + $arguments + ' -Visible'
            $windowStyle = 'Normal'
        }
        if ($VerifyLogin -and $part -eq 'Client') { $arguments += ' -VerifyLogin' }
        $process = Start-Process powershell.exe -ArgumentList $arguments -WindowStyle $windowStyle -PassThru
        $sessions += [PSCustomObject]@{
            Component = $part
            ProcessId = $process.Id
            StartedAtUtc = $process.StartTime.ToUniversalTime().ToString('o')
        }
        $sessions | ConvertTo-Json | Set-Content (Join-Path $tools 'local-session.json')
    }
    if ($Component -eq 'All') { Write-Host 'Started all components from main/.' }
    else { Write-Host 'Started update and game servers.' }
    Write-Host 'Server/client logs are in .tools. DEV login is local-only; use disposable test credentials.'
    Write-Host 'Stop components with scripts/stop-local.ps1.'
    exit 0
}

switch ($Component) {
    'UpdateServer' { $directory = $source; $task = ':llyeta-update-server:run' }
    'GameServer' { $directory = $source; $task = ':llyeta-server:run' }
    'Client' { $directory = Join-Path $source 'runelite'; $task = 'run' }
}
$tasks = @($task)
if ($BuildOnly) {
    $tasks = @(if ($Component -eq 'Client') { ':runelite-client:classes' } else { $task -replace ':run$', ':classes' })
}
if ($VerifyLogin -and $Component -eq 'Client') {
    $classes = Join-Path $tools 'smoke-classes'
    New-Item -ItemType Directory -Force $classes | Out-Null
    & (Join-Path $env:JAVA_HOME 'bin/javac.exe') -d $classes (Join-Path $PSScriptRoot 'LocalLoginSmoke.java')
    if ($LASTEXITCODE -ne 0) { throw 'Smoke probe compilation failed.' }
    $tasks += @('--init-script', (Join-Path $PSScriptRoot 'local-smoke.init.gradle'))
}
$logName = if ($Component -eq 'Client') { "main-Client-$PID.log" } else { "main-$Component.log" }
Invoke-LocalGradle $directory $tasks (Join-Path $tools $logName)
