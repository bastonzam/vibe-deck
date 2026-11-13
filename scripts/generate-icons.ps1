param([string]$Source = 'app/src/main/assets/mock-icon/mock-neon.png')
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
$projectRoot = Split-Path $PSScriptRoot -Parent
$sourcePath = Join-Path $projectRoot $Source
$image = [System.Drawing.Image]::FromFile($sourcePath)
function Write-Icon([string]$Destination, [int]$Size, [bool]$Round) {
    $renderSize = $Size * 4
    $large = New-Object System.Drawing.Bitmap($renderSize, $renderSize, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $graphics = [System.Drawing.Graphics]::FromImage($large)
    try {
        $backdrop = if ($Round) { [System.Drawing.Color]::Transparent } else { $image.GetPixel(0, 0) }
        $graphics.Clear($backdrop)
        $graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
        $graphics.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
        $graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
        if ($Round) {
            $path = New-Object System.Drawing.Drawing2D.GraphicsPath
            try { $path.AddEllipse(0, 0, $renderSize, $renderSize); $graphics.SetClip($path) } finally { $path.Dispose() }
        }
        # Uniform fit preserves the source artwork without distorting or cropping the logo.
        $scale = [Math]::Min($renderSize / $image.Width, $renderSize / $image.Height)
        $width = [int]($image.Width * $scale)
        $height = [int]($image.Height * $scale)
        $graphics.DrawImage($image, [int](($renderSize - $width) / 2), [int](($renderSize - $height) / 2), $width, $height)
        $format = if ($Round) { [System.Drawing.Imaging.PixelFormat]::Format32bppArgb } else { [System.Drawing.Imaging.PixelFormat]::Format24bppRgb }
        $output = New-Object System.Drawing.Bitmap($Size, $Size, $format)
        $resizer = [System.Drawing.Graphics]::FromImage($output)
        try {
            $resizer.Clear($backdrop)
            $resizer.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
            $resizer.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
            $resizer.DrawImage($large, 0, 0, $Size, $Size)
            $output.Save($Destination, [System.Drawing.Imaging.ImageFormat]::Png)
        } finally { $resizer.Dispose(); $output.Dispose() }
    } finally { $graphics.Dispose(); $large.Dispose() }
}
try {
    foreach ($density in @(@('mdpi',48), @('hdpi',72), @('xhdpi',96), @('xxhdpi',144), @('xxxhdpi',192))) {
        $directory = Join-Path $projectRoot ('app/src/main/res/mipmap-' + $density[0])
        New-Item -ItemType Directory -Force $directory | Out-Null
        Write-Icon (Join-Path $directory 'ic_launcher.png') $density[1] $false
        Write-Icon (Join-Path $directory 'ic_launcher_round.png') $density[1] $true
    }
    Write-Icon (Join-Path $projectRoot 'app/src/main/res/ic_launcher-web.png') 512 $false
} finally { $image.Dispose() }
Get-ChildItem (Join-Path $projectRoot 'app/src/main/res') -Recurse -Filter 'ic_launcher*.png' | ForEach-Object {
    $icon = [System.Drawing.Image]::FromFile($_.FullName)
    try { Write-Output "$($_.FullName): $($icon.Width)x$($icon.Height)" } finally { $icon.Dispose() }
}
