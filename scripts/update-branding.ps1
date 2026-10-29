$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
$workspace = Split-Path $PSScriptRoot -Parent
$branding = Join-Path $workspace 'main/branding'
$resources = Join-Path $workspace 'main/runelite/runelite-client/src/main/resources'

function Render-Asset([string]$InputFile, [int]$Width, [int]$Height, [bool]$Cover) {
    $inputImage = [Drawing.Image]::FromFile($InputFile)
    try {
        $bitmap = New-Object Drawing.Bitmap($Width, $Height)
        $graphics = [Drawing.Graphics]::FromImage($bitmap)
        try {
            $graphics.Clear([Drawing.Color]::Transparent)
            $graphics.InterpolationMode = [Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
            $graphics.PixelOffsetMode = [Drawing.Drawing2D.PixelOffsetMode]::HighQuality
            $scale = if ($Cover) { [Math]::Max($Width / $inputImage.Width, $Height / $inputImage.Height) }
                else { [Math]::Min($Width / $inputImage.Width, $Height / $inputImage.Height) }
            $drawWidth = [int][Math]::Round($inputImage.Width * $scale)
            $drawHeight = [int][Math]::Round($inputImage.Height * $scale)
            $graphics.DrawImage($inputImage, [int](($Width - $drawWidth) / 2), [int](($Height - $drawHeight) / 2), $drawWidth, $drawHeight)
        } finally { $graphics.Dispose() }
        return ,$bitmap
    } finally { $inputImage.Dispose() }
}

$background = Render-Asset (Join-Path $branding 'login-background.png') 765 503 $true
try {
    foreach ($half in @(@('leftTitleBackground.png', 0, 383), @('rightTitleBackground.png', 383, 382))) {
        $rectangle = New-Object Drawing.Rectangle([int]$half[1], 0, [int]$half[2], 503)
        $image = $background.Clone($rectangle, [Drawing.Imaging.PixelFormat]::Format32bppArgb)
        try { $image.Save((Join-Path $resources ('img/' + $half[0])), [Drawing.Imaging.ImageFormat]::Png) }
        finally { $image.Dispose() }
    }
} finally { $background.Dispose() }

$icon = Render-Asset (Join-Path $branding 'app-icon.png') 191 225 $false
try { $icon.Save((Join-Path $resources 'lletya.png'), [Drawing.Imaging.ImageFormat]::Png) }
finally { $icon.Dispose() }

$wordmark = Render-Asset (Join-Path $branding 'launcher-wordmark.png') 469 167 $false
try { $wordmark.Save((Join-Path $workspace 'main/lletya-launcher/src/main/resources/logo.png'), [Drawing.Imaging.ImageFormat]::Png) }
finally { $wordmark.Dispose() }

Write-Host 'Updated the native login halves, application icon and launcher wordmark.'
