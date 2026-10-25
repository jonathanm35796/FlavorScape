$ErrorActionPreference = 'Stop'

if (-not (Get-Command winget -ErrorAction SilentlyContinue)) {
    throw 'Windows Package Manager is unavailable. Install or repair Microsoft App Installer, then run this script again.'
}

foreach ($package in @(
    @{ Command = 'git'; Id = 'Git.Git' },
    @{ Command = 'gh'; Id = 'GitHub.cli' }
)) {
    if (Get-Command $package.Command -ErrorAction SilentlyContinue) {
        Write-Host "$($package.Command) is already on PATH."
        continue
    }
    & winget install --id $package.Id --exact --source winget --accept-source-agreements --accept-package-agreements
    if ($LASTEXITCODE -ne 0) {
        throw "Installation failed for $($package.Id), exit code $LASTEXITCODE."
    }
}

Write-Host 'Reopen PowerShell to refresh PATH. Run git --version and gh --version, then gh auth login.'
Write-Host 'Java selection is pending inspection of candidate build requirements.'
