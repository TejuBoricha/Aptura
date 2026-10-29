if (-not $env:APTURA_DEVICE_WORK) { $env:APTURA_DEVICE_WORK = Join-Path $env:TEMP 'aptura-device-test' }
New-Item -ItemType Directory -Force $env:APTURA_DEVICE_WORK | Out-Null
Add-Type -AssemblyName System.Drawing
$script:adb = $(if ($env:ADB) { $env:ADB } else { 'adb' })
$script:sp  = "$env:APTURA_DEVICE_WORK"

# Full screenshot to $sp\$name.png
function Snap([string]$name) {
    & $script:adb shell screencap -p /sdcard/s.png
    & $script:adb pull /sdcard/s.png "$script:sp\$name.png" | Out-Null
    & $script:adb shell rm /sdcard/s.png
}

# Screenshot cropped to a pixel rectangle, saved as $sp\$name.png (keeps the page small to read).
function Snap-Crop([string]$name, [int]$x, [int]$y, [int]$w, [int]$h) {
    Snap "_full"
    $src = [System.Drawing.Bitmap]::FromFile("$script:sp\_full.png")
    $rect = New-Object System.Drawing.Rectangle $x, $y, $w, $h
    $dst = $src.Clone($rect, $src.PixelFormat)
    $dst.Save("$script:sp\$name.png", [System.Drawing.Imaging.ImageFormat]::Png)
    $dst.Dispose(); $src.Dispose()
}

# Stack several cropped PNGs (same width) top to bottom into one image with a gap.
function Stack-Images([string[]]$names, [string]$outName) {
    $imgs = $names | ForEach-Object { [System.Drawing.Bitmap]::FromFile("$script:sp\$_.png") }
    [int]$w = ($imgs | Measure-Object -Property Width -Maximum).Maximum
    [int]$h = ($imgs | Measure-Object -Property Height -Sum).Sum + 8 * ($imgs.Count - 1)
    $bmp = New-Object System.Drawing.Bitmap ([int]$w), ([int]$h)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.Clear([System.Drawing.Color]::DimGray)
    $y = 0
    foreach ($i in $imgs) { $g.DrawImage($i, 0, $y); $y += $i.Height + 8 }
    $bmp.Save("$script:sp\$outName.png", [System.Drawing.Imaging.ImageFormat]::Png)
    $g.Dispose(); $bmp.Dispose(); $imgs | ForEach-Object { $_.Dispose() }
}

# Underrun counter for the app's active audio track, or $null when there is none.
function Get-Underruns {
    $pid1 = (& $script:adb shell pidof org.aptura.player).Trim()
    $row = ((& $script:adb shell dumpsys media.audio_flinger) | Select-String -Pattern "yes\s+$pid1/" | Select-Object -First 1)
    if ($row) { ($row.Line.Trim() -split "\s+")[24] } else { $null }
}

# Aptura's own playback state ("PLAYING pos=140475"), not whichever session dumpsys lists first.
function Get-AptState {
    $d = & $script:adb shell dumpsys media_session
    $i = 0; for ($k = 0; $k -lt $d.Count; $k++) { if ($d[$k] -match 'androidx\.media3\.session\.id\. org\.aptura\.player/') { $i = $k; break } }
    if (-not $i) { return "no aptura session" }
    for ($k = $i; $k -lt [Math]::Min($i + 25, $d.Count); $k++) { if ($d[$k] -match 'state=PlaybackState \{state=(\w+)\(\d+\), position=(-?\d+)') { return "{0} pos={1}" -f $Matches[1], $Matches[2] } }
    "no state line"
}
