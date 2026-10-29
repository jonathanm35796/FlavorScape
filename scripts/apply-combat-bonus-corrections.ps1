param()
$ErrorActionPreference = 'Stop'
$workspace = Split-Path $PSScriptRoot -Parent
$audit = Join-Path $workspace '.tools/combat-audit'
$path = Join-Path $workspace 'main/lletya-server/data/items/item_info.json'
$differences = Import-Csv (Join-Path $audit 'bonus-differences.tsv') -Delimiter "`t"
$byId = @{}
foreach ($row in $differences) {
    if ($row.field -notmatch '^[a-z_]+_bonus$') { throw "Unexpected bonus field $($row.field)" }
    if (!$byId.ContainsKey($row.id)) { $byId[$row.id] = @() }
    $byId[$row.id] += $row
}
$raw = [IO.File]::ReadAllText($path)
$backup = Join-Path $audit 'item_info.before-bonus-corrections.json'
if (!(Test-Path -LiteralPath $backup)) { [IO.File]::WriteAllText($backup, $raw) }
$changedIds = [Collections.Generic.HashSet[string]]::new()
$updated = [regex]::Replace($raw, '(?ms)^[\t ]*\{.*?^[\t ]*\}', { param($block)
    $text = $block.Value
    $idMatch = [regex]::Match($text, '"id"\s*:\s*(\d+)')
    $id = $idMatch.Groups[1].Value
    if (!$byId.ContainsKey($id)) { return $text }
    foreach ($row in $byId[$id]) {
        $fieldPattern = '"' + $row.field + '"\s*:\s*(-?\d+(?:\.\d+)?)'
        $fieldMatch = [regex]::Match($text, $fieldPattern)
        $old = if ($fieldMatch.Success) { [double]::Parse($fieldMatch.Groups[1].Value, [Globalization.CultureInfo]::InvariantCulture) } else { 0 }
        if ($old -ne [double]::Parse($row.server, [Globalization.CultureInfo]::InvariantCulture)) { throw "Stale audit for item $id / $($row.field)" }
        $number = [double]::Parse($row.wiki, [Globalization.CultureInfo]::InvariantCulture).ToString('0.###', [Globalization.CultureInfo]::InvariantCulture)
        $replacement = '"' + $row.field + '": ' + $number
        if ($fieldMatch.Success) { $text = [regex]::Replace($text, $fieldPattern, $replacement) }
        else { $text = [regex]::Replace($text, '("id"\s*:\s*\d+,)', ('$1' + "`r`n    " + $replacement + ',')) }
    }
    $null = $changedIds.Add($id)
    return $text
})
if ($changedIds.Count -ne $byId.Count) { throw "Only found $($changedIds.Count) of $($byId.Count) item blocks; nothing written." }
[IO.File]::WriteAllText($path, $updated)
Write-Output "Corrected $($differences.Count) bonus values across $($changedIds.Count) equipment entries. Backup: $backup"
