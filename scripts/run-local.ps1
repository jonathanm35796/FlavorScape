param(
    [ValidateSet('All', 'UpdateServer', 'GameServer', 'Client')]
    [string]$Component = 'All',
    [switch]$BuildOnly,
    [switch]$VerifyLogin
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

function Invoke-LocalGradle([string]$Directory, [string[]]$Tasks, [string]$Log) {
    Push-Location $Directory
    try {
        # Windows PowerShell wraps native stderr as errors; Gradle's exit code is authoritative.
        $ErrorActionPreference = 'Continue'
        & .\gradlew.bat @Tasks --console=plain --no-daemon '-Dorg.gradle.warning.mode=summary' *> $Log
        $result = $LASTEXITCODE
        if ($result -ne 0) { Get-Content $Log -Tail 40; throw "Gradle failed ($result). See $Log" }
    } finally { Pop-Location }
}

if ($Component -eq 'All') {
    if (-not $BuildOnly) {
        $listeners = Get-NetTCPConnection -State Listen -ErrorAction SilentlyContinue |
            Where-Object { $_.LocalPort -in @(7304, 13302) }
        if ($listeners) { throw 'Local game ports are already in use. Run scripts/stop-local.ps1 first.' }
    }
    Write-Host 'Building game and update servers with JDK 21...'
    Invoke-LocalGradle $source @(':flavorscape-server:classes', ':flavorscape-update-server:classes') (Join-Path $tools 'main-server-build.log')
    $env:JAVA_HOME = (Get-ChildItem (Join-Path $tools 'jdk8') -Directory | Select-Object -First 1).FullName
    Write-Host 'Building client with JDK 8...'
    Invoke-LocalGradle (Join-Path $source 'runelite') @(':runelite-client:classes') (Join-Path $tools 'main-client-build.log')
    if ($BuildOnly) { Write-Host 'All components compiled successfully.'; exit 0 }
    $sessions = @()
    foreach ($part in @('UpdateServer', 'GameServer', 'Client')) {
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
        if ($VerifyLogin -and $part -eq 'Client') { $arguments += ' -VerifyLogin' }
        $process = Start-Process powershell.exe -ArgumentList $arguments -WindowStyle Hidden -PassThru
        $sessions += [PSCustomObject]@{ Component = $part; ProcessId = $process.Id }
        $sessions | ConvertTo-Json | Set-Content (Join-Path $tools 'local-session.json')
    }
    Write-Host 'Started all components from main/. Logs: .tools/main-UpdateServer.log, main-GameServer.log, main-Client.log.'
    Write-Host 'DEV login is local-only: use a disposable test username/password. Stop with scripts/stop-local.ps1.'
    exit 0
}

switch ($Component) {
    'UpdateServer' { $directory = $source; $task = ':flavorscape-update-server:run' }
    'GameServer' { $directory = $source; $task = ':flavorscape-server:run' }
    'Client' { $directory = Join-Path $source 'runelite'; $task = 'run' }
}
$tasks = @($task)
if ($VerifyLogin -and $Component -eq 'Client') {
    $classes = Join-Path $tools 'smoke-classes'
    New-Item -ItemType Directory -Force $classes | Out-Null
    & (Join-Path $env:JAVA_HOME 'bin/javac.exe') -d $classes (Join-Path $PSScriptRoot 'LocalLoginSmoke.java')
    if ($LASTEXITCODE -ne 0) { throw 'Smoke probe compilation failed.' }
    $tasks += @('--init-script', (Join-Path $PSScriptRoot 'local-smoke.init.gradle'))
}
Invoke-LocalGradle $directory $tasks (Join-Path $tools "main-$Component.log")
