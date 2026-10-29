param([int]$Minutes = 30)
. "$PSScriptRoot\snap.ps1"
$adb = $script:adb
$log = Join-Path $script:sp "thermal-run.csv"
"elapsed_s,clock,state,pos_ms,expected_pos_ms,drift_ms,thermal_status,ap_c,skin_c,battery_c,battery_pct,underruns,cpu_big_khz" | Set-Content $log -Encoding UTF8

function Thermal {
    $t = & $adb shell dumpsys thermalservice
    $status = ($t | Select-String "Thermal Status: (\d+)" | Select-Object -First 1)
    $st = if ($status) { $status.Matches[0].Groups[1].Value } else { "?" }
    $hi = ($t | Select-String "Current temperatures from HAL" | Select-Object -First 1).LineNumber
    $sec = if ($hi) { $t[$hi..([Math]::Min($hi + 10, $t.Count - 1))] } else { @() }
    $temps = @{}
    $sec | Select-String "Temperature\{mValue=([\d\.\-]+), mType=\d+, mName=([A-Za-z0-9_\-]+)" -AllMatches | ForEach-Object { $_.Matches } | ForEach-Object { if (-not $temps.ContainsKey($_.Groups[2].Value)) { $temps[$_.Groups[2].Value] = $_.Groups[1].Value } }
    [pscustomobject]@{ Status = $st; AP = $temps["AP"]; Skin = ($temps["SKIN"], $temps["skin"], $temps["BACK"] | Where-Object { $_ } | Select-Object -First 1) }
}

function Get-RealPos {
    # media_session stores position + the time it was updated; real position = position + (now - updated) * speed.
    $d = & $adb shell dumpsys media_session
    $i = 0; for ($k = 0; $k -lt $d.Count; $k++) { if ($d[$k] -match 'androidx\.media3\.session\.id\. org\.aptura\.player/') { $i = $k; break } }
    if (-not $i) { return -1 }
    for ($k = $i; $k -lt [Math]::Min($i + 25, $d.Count); $k++) {
        if ($d[$k] -match 'state=PlaybackState \{state=(\w+)\(\d+\), position=(-?\d+), buffered position=\d+, speed=([\d\.]+), updated=(\d+)') {
            $p = [double]$Matches[2]; $sp2 = [double]$Matches[3]; $up = [double]$Matches[4]
            if ($Matches[1] -ne 'PLAYING') { return [int]$p }
            $al = & $adb shell dumpsys alarm | Select-String 'nowELAPSED=(\d+)' | Select-Object -First 1
            if (-not $al) { return [int]$p }
            $now = [double]$al.Matches[0].Groups[1].Value
            return [int]($p + ($now - $up) * $sp2)
        }
    }
    -1
}
$start = Get-Date
$first = $null
$end = $start.AddMinutes($Minutes)
$i = 0
while ((Get-Date) -lt $end) {
    $now = Get-Date
    $el = [int]($now - $start).TotalSeconds
    $stLine = Get-AptState
    $state = ($stLine -split ' ')[0]
    $pos = Get-RealPos
    if ($null -eq $first -and $pos -ge 0) { $first = @{ pos = $pos; t = $now } }
    $expected = if ($first) { (($first.pos + ($now - $first.t).TotalMilliseconds) % 60000) } else { -1 }
    $drift = if ($first -and $pos -ge 0) { $d = $pos - $expected; if ($d -gt 30000) { $d -= 60000 } elseif ($d -lt -30000) { $d += 60000 }; [int]$d } else { "" }
    $th = Thermal
    $bat = & $adb shell dumpsys battery
    $bt = ($bat | Select-String "temperature: (\d+)" | Select-Object -First 1).Matches[0].Groups[1].Value
    $bp = ($bat | Select-String "level: (\d+)" | Select-Object -First 1).Matches[0].Groups[1].Value
    $under = Get-Underruns
    $cpu = (& $adb shell cat /sys/devices/system/cpu/cpu7/cpufreq/scaling_cur_freq 2>$null)
    ("{0},{1},{2},{3},{4},{5},{6},{7},{8},{9},{10},{11},{12}" -f $el, $now.ToString("HH:mm:ss"), $state, $pos, [int]$expected, $drift, $th.Status, $th.AP, $th.Skin, ([double]$bt / 10), $bp, $under, $cpu) | Add-Content $log -Encoding UTF8
    if ($i % 10 -eq 0) { Snap-Crop ("thermal_stats_{0:D2}" -f ($i / 10)) 0 500 1080 520 }
    $i++
    $wait = 30 - ((Get-Date) - $now).TotalSeconds
    if ($wait -gt 0) { Start-Sleep -Seconds $wait }
}
"DONE " + (Get-Date).ToString("HH:mm:ss") | Add-Content $log -Encoding UTF8

