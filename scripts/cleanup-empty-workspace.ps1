param(
    [Parameter(Mandatory = $true)]
    [string]$OriginalDirectory,
    [ValidateRange(0, 86400)]
    [int]$WaitSeconds = 7200
)

$ErrorActionPreference = 'Stop'
$current = [IO.Path]::GetFullPath((Split-Path $PSScriptRoot -Parent)).TrimEnd('\')
$original = [IO.Path]::GetFullPath($OriginalDirectory).TrimEnd('\')
if ($original -eq $current -or
    [IO.Path]::GetDirectoryName($original) -ne [IO.Path]::GetDirectoryName($current) -or
    -not (Test-Path -LiteralPath (Join-Path $current '.git'))) {
    throw 'The original directory must be a different sibling of this repository.'
}
$deadline = [DateTime]::UtcNow.AddSeconds($WaitSeconds)
do {
    if (-not [IO.Directory]::Exists($original)) { exit 0 }
    if ([IO.Directory]::GetFileSystemEntries($original).Length -ne 0) {
        throw 'The original directory is no longer empty; it was left untouched.'
    }
    try {
        [IO.Directory]::Delete($original, $false)
        Write-Output 'Removed the empty original workspace directory.'
        exit 0
    } catch [IO.IOException] {
        if ([DateTime]::UtcNow -ge $deadline) { throw }
        Start-Sleep -Seconds 5
    }
} while ([DateTime]::UtcNow -lt $deadline)
throw 'The original directory is still locked; close the old editor/tool sessions and rerun.'
