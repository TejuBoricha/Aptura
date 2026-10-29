if (-not $env:APTURA_DEVICE_WORK) { $env:APTURA_DEVICE_WORK = Join-Path $env:TEMP 'aptura-device-test' }
New-Item -ItemType Directory -Force $env:APTURA_DEVICE_WORK | Out-Null
$script:adb = $(if ($env:ADB) { $env:ADB } else { 'adb' })
$script:tmp = "$env:APTURA_DEVICE_WORK\ui.xml"

# Dump visible UI nodes. Works while the player is paused; while video plays the dump is empty.
function Get-Ui([switch]$All) {
    Remove-Item $script:tmp -ErrorAction SilentlyContinue
    & $script:adb shell uiautomator dump /sdcard/ui.xml 2>&1 | Out-Null
    & $script:adb pull /sdcard/ui.xml $script:tmp 2>&1 | Out-Null
    if (-not (Test-Path $script:tmp)) { return @() }
    [xml]$x = Get-Content $script:tmp -Raw -Encoding UTF8
    $x.SelectNodes("//node") | Where-Object { $All -or $_.text -or $_.'content-desc' } | ForEach-Object {
        $b = [regex]::Match($_.bounds, '\[(\d+),(\d+)\]\[(\d+),(\d+)\]')
        $l = [int]$b.Groups[1].Value; $t = [int]$b.Groups[2].Value; $r = [int]$b.Groups[3].Value; $btm = [int]$b.Groups[4].Value
        [pscustomobject]@{
            Text  = $_.text
            Desc  = $_.'content-desc'
            Click = $_.clickable
            Sel   = $_.selected
            Chk   = $_.checked
            Class = ($_.class -split '\.')[-1]
            W     = $r - $l
            H     = $btm - $t
            X     = [int](($l + $r) / 2)
            Y     = [int](($t + $btm) / 2)
        }
    }
}

function Show-Ui { Get-Ui | Format-Table Text, Desc, Click, Sel, Chk, Class, W, H, X, Y -AutoSize -Wrap | Out-String -Width 260 }

# Tap the first node whose text or description matches the regex.
function Tap-Ui([string]$pattern) {
    $n = Get-Ui | Where-Object { $_.Text -match $pattern -or $_.Desc -match $pattern } | Select-Object -First 1
    if (-not $n) { "NOT FOUND: $pattern"; return $false }
    & $script:adb shell input tap $n.X $n.Y | Out-Null
    "tapped '$($n.Text)$($n.Desc)' at $($n.X),$($n.Y)"
    return $true
}

# Current audio-sync value text on the Audio sheet ("0 ms", "+50 ms", ...).
function Get-SyncValue {
    (Get-Ui | Where-Object { $_.Text -match '^[+\u2212-]?\d+ ms$' } | Select-Object -First 1).Text
}

# Press and hold a point for the given milliseconds (a single touch, no movement).
function Hold([int]$x, [int]$y, [int]$ms) { & $script:adb shell input swipe $x $y $x $y $ms | Out-Null }
