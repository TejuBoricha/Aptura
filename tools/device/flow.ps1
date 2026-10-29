. "$PSScriptRoot\ui.ps1"
. "$PSScriptRoot\snap.ps1"

$script:ids = @{ sleep = 309337; hdr = 309342; other = 309343; pcm24 = 309344; pcm51 = 309345; pcmf32 = 309346; uhd = 309348 }

# Open a test video in Aptura the way a file manager would (read grant on the content URI).
function Open-Video([string]$key) {
    & $script:adb shell am start --grant-read-uri-permission -a android.intent.action.VIEW -d "content://media/external/video/media/$($script:ids[$key])" -t video/mp4 -p org.aptura.player | Out-Null
    Start-Sleep -Milliseconds 3000
}

function Pause-Aptura { if ((Get-AptState) -match '^PLAYING') { & $script:adb shell input keyevent 127; Start-Sleep -Milliseconds 800 } }

# True once the player's control bar is on screen (needs the player to be paused so the UI dump works).
function Has-Controls { [bool](Get-Ui | Where-Object { $_.Desc -eq 'Audio Track' }) }

function Ensure-Controls {
    if (Has-Controls) { return $true }
    & $script:adb shell input tap 540 1170; Start-Sleep -Milliseconds 700
    Has-Controls
}

# Open the Audio sheet from the player (pauses first so the dump works).
function Open-AudioSheet {
    Pause-Aptura
    if (-not (Ensure-Controls)) { return "no controls" }
    Tap-Ui '^Audio Track$' | Out-Null; Start-Sleep -Milliseconds 1200
    if (Get-Ui | Where-Object { $_.Text -eq 'AUDIO SYNC' }) { "audio sheet open" } else { "audio sheet NOT open" }
}

function Close-Sheet {
    Pause-Aptura
    $h = Get-Ui | Where-Object { $_.Desc -eq 'Drag handle' } | Select-Object -First 1
    if ($h) { & $script:adb shell input swipe $h.X $h.Y $h.X 2250 350; Start-Sleep -Milliseconds 1000 }
}

# From the library grid, open a test video by its title (the tile's accessibility label).
function Open-FromLibrary([string]$name) {
    $r = Tap-Ui "^$([regex]::Escape($name))$"
    Start-Sleep -Milliseconds 3000
    $r
}

# From the player, go back to the library grid.
function Leave-Player {
    Close-Sheet; Pause-Aptura
    if (Ensure-Controls) { Tap-Ui '^Back$' | Out-Null }
    Start-Sleep -Milliseconds 2000
    [bool](Get-Ui | Where-Object { $_.Desc -eq 'Search' })
}

function Tap-Plus([int]$n) { 1..$n | ForEach-Object { & $script:adb shell input tap 922 1980; Start-Sleep -Milliseconds 250 } }


# Title of the item Aptura's media session is playing (guards against queue auto-advance).
function Get-AptTitle {
    $d = & $script:adb shell dumpsys media_session
    for ($k = 0; $k -lt $d.Count; $k++) { if ($d[$k] -match 'androidx\.media3\.session\.id\. org\.aptura\.player/') { for ($j = $k; $j -lt $k + 40; $j++) { if ($d[$j] -match 'metadata: size=\d+, description=([^,]+),') { return $Matches[1] } }; break } }
    "?"
}

function Get-FlingerFormat {
    $pid1 = (& $script:adb shell pidof org.aptura.player).Trim()
    $row = ((& $script:adb shell dumpsys media.audio_flinger) | Select-String -Pattern "yes\s+$pid1/" | Select-Object -First 1)
    if ($row) { $f = ($row.Line.Trim() -split "\s+"); "flinger format=0x$($f[8]) chanmask=0x$($f[9]) rate=$($f[10]) gain_dB=$($f[14]) underruns=$($f[24])" } else { "no active audio track" }
}

function Test-Pcm([string]$name) {
    "=== $name ==="
    & $script:adb logcat -c
    if (-not (Get-Ui | Where-Object { $_.Desc -eq 'Search' })) { "to library: " + (Leave-Player) }
    Open-FromLibrary $name | Out-Null
    $s = Open-AudioSheet; $v = Get-SyncValue
    if ($v -eq '0 ms') { Tap-Plus 4; Start-Sleep -Milliseconds 1200; $v = Get-SyncValue }
    "sheet: $s; audio offset: $v"
    Close-Sheet
    & $script:adb shell input keyevent 126; Start-Sleep -Milliseconds 4500
    "title: " + (Get-AptTitle) + "; state: " + (Get-AptState)
    Get-FlingerFormat
    Snap-Crop "stats_$name" 0 150 1080 1000
    "crashes in log: " + ((& $script:adb logcat -d -b crash | Select-String "aptura|FATAL" | Measure-Object).Count)
}

# Print every attribute of the first nodes whose text or description matches (uses the last Get-Ui dump).
function Show-Raw([string]$pattern, [int]$max = 3) {
    [xml]$x = Get-Content $script:tmp -Raw -Encoding UTF8
    $x.SelectNodes("//node") | Where-Object { $_.text -match $pattern -or $_.'content-desc' -match $pattern } | Select-Object -First $max | ForEach-Object {
        $a = $_.Attributes | Where-Object { $_.Value -ne '' -and $_.Value -ne 'false' -and $_.Name -notin 'bounds','index','package' } | ForEach-Object { "$($_.Name)=$($_.Value)" }
        "  " + ($a -join "  ")
    }
}

# Start a custom sleep timer (replaces any running one). Leaves the player paused with the sheet closed.
function Set-SleepTimerMinutes([int]$min) {
    Pause-Aptura
    $null = Ensure-Controls
    Tap-Ui '^Advanced Settings$' | Out-Null; Start-Sleep -Milliseconds 1500
    $f = $false
    for ($i = 0; $i -lt 8 -and -not $f; $i++) {
        $h = Get-Ui | Where-Object { $_.Text -match '^Custom' } | Select-Object -First 1
        if ($h -and $h.Y -gt 300 -and $h.Y -lt 1900) { $f = $true } else { & $script:adb shell input swipe 300 2000 300 1300 450; Start-Sleep -Milliseconds 600 }
    }
    Tap-Ui '^Custom' | Out-Null; Start-Sleep -Milliseconds 1100
    $fld = Get-Ui -All | Where-Object { $_.Class -eq 'EditText' } | Select-Object -First 1
    if ($fld) { & $script:adb shell input tap $fld.X $fld.Y } else { & $script:adb shell input tap 538 1300 }
    Start-Sleep -Milliseconds 800; & $script:adb shell input text $min; Start-Sleep -Milliseconds 500
    $s = Get-Ui | Where-Object { $_.Text -eq 'Start timer' } | Select-Object -First 1
    & $script:adb shell input tap $s.X $s.Y
    $script:t0 = Get-Date
    Set-Content "$env:APTURA_DEVICE_WORK\t0.txt" ($script:t0.ToString("o"))
    Start-Sleep -Milliseconds 800
    "timer set to $min min at " + $script:t0.ToString("HH:mm:ss") + "; card: " + ((Get-Ui | Where-Object { $_.Text -match 'Stops in' }).Text)
    Close-Sheet
}

# Print what the player and audio system are doing, only when something changes (or every 20 s).
function Watch-Playback([datetime]$from, [int]$seconds) {
    $last = ""; $until = $from.AddSeconds($seconds)
    while ((Get-Date) -lt $until) {
        $off = [math]::Round(((Get-Date) - $from).TotalSeconds, 0)
        $pid1 = (& $script:adb shell pidof org.aptura.player).Trim()
        $row = if ($pid1) { ((& $script:adb shell dumpsys media.audio_flinger) | Select-String -Pattern "yes\s+$pid1/" | Select-Object -First 1) } else { $null }
        $line = if (-not $pid1) { "PROCESS GONE" } elseif ($row) { "audio vol=" + (($row.Line.Trim() -split "\s+")[28]) + " | " + (Get-AptState) } else { "no audio track | " + (Get-AptState) }
        if ($line -ne $last -or ($off % 20 -eq 0)) { "t+{0,3}s  {1}" -f $off, $line; $last = $line }
        Start-Sleep -Milliseconds 800
    }
}

# Open one of my test clips through the library search box (never taps anything but the tile with the exact title).
function Open-ByName([string]$name) {
    if (-not (Get-Ui | Where-Object { $_.Desc -eq 'Search' })) { Leave-Player | Out-Null }
    $null = Tap-Ui '^Clear search$'; Start-Sleep -Milliseconds 500
    Tap-Ui '^Search$' | Out-Null; Start-Sleep -Milliseconds 1000
    & $script:adb shell input text $name; Start-Sleep -Milliseconds 1800
    $tile = Get-Ui | Where-Object { $_.Desc -eq $name -and $_.Y -gt 400 } | Select-Object -First 1
    if (-not $tile) { return "tile not found: $name" }
    & $script:adb shell input tap $tile.X $tile.Y; Start-Sleep -Milliseconds 3000
    "opened $name -> title now: " + (Get-AptTitle)
}

# Turn the Playback stats overlay on or off (needs the player paused with controls reachable).
function Set-StatsOverlay([bool]$on) {
    Pause-Aptura; $null = Ensure-Controls
    Tap-Ui '^Advanced Settings$' | Out-Null; Start-Sleep -Milliseconds 1500
    $f = $false
    for ($i = 0; $i -lt 10 -and -not $f; $i++) {
        $h = Get-Ui | Where-Object { $_.Text -eq 'Playback stats' } | Select-Object -First 1
        if ($h -and $h.Y -gt 300 -and $h.Y -lt 2100) { $f = $true } else { & $script:adb shell input swipe 300 2100 300 900 450; Start-Sleep -Milliseconds 600 }
    }
    $null = Get-Ui; [xml]$x = Get-Content $script:tmp -Raw -Encoding UTF8
    $row = $x.SelectNodes("//node[@checkable='true']") | Where-Object { $_.SelectNodes(".//node") | Where-Object { $_.text -eq 'Playback stats' } } | Select-Object -First 1
    $isOn = ($row.checked -eq 'true')
    if ($isOn -ne $on) { Tap-Ui '^Playback stats$' | Out-Null; Start-Sleep -Milliseconds 700 }
    Close-Sheet
    "stats overlay was " + $(if ($isOn) { 'on' } else { 'off' }) + ", now " + $(if ($on) { 'on' } else { 'off' })
}
