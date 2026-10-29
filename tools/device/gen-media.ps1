if (-not $env:APTURA_DEVICE_WORK) { $env:APTURA_DEVICE_WORK = Join-Path $env:TEMP 'aptura-device-test' }
New-Item -ItemType Directory -Force $env:APTURA_DEVICE_WORK | Out-Null
$ErrorActionPreference = "Stop"
$ff  = $(if ($env:FFMPEG) { $env:FFMPEG } else { 'ffmpeg' })
$adb = $(if ($env:ADB) { $env:ADB } else { 'adb' })
$sp  = "$env:APTURA_DEVICE_WORK"
$out = Join-Path $sp "media"
New-Item -ItemType Directory -Force $out | Out-Null

function Run($name, [string[]]$ffArgs) {
    "[{0}] encoding {1}" -f (Get-Date -Format HH:mm:ss), $name
    & $ff -y -loglevel error @ffArgs (Join-Path $out $name)
    if ($LASTEXITCODE -ne 0) { throw "ffmpeg failed for $name" }
    "[{0}] done {1} ({2:N1} MB)" -f (Get-Date -Format HH:mm:ss), $name, ((Get-Item (Join-Path $out $name)).Length / 1MB)
}

# second ordinary video, for the "switch to another video and back" persistence check
Run "other-test.mp4" @("-f","lavfi","-i","smptebars=size=640x360:rate=30:duration=60","-f","lavfi","-i","sine=frequency=660:sample_rate=44100:duration=60","-af","volume=0.15","-c:v","libx264","-preset","veryfast","-pix_fmt","yuv420p","-c:a","aac","-b:a","96k","-shortest")

# sidecar subtitles for sleep-test.mp4: a 2 s cue every 5 s over 240 s
$srt = New-Object System.Text.StringBuilder
for ($i = 0; $i -lt 48; $i++) {
    $s = 5 * ($i + 1); $e = $s + 2
    $fmt = { param($t) "{0:00}:{1:00}:{2:00},000" -f [int][math]::Floor($t / 3600), [int][math]::Floor(($t % 3600) / 60), [int]($t % 60) }
    [void]$srt.AppendLine($i + 1)
    [void]$srt.AppendLine(("{0} --> {1}" -f (& $fmt $s), (& $fmt $e)))
    [void]$srt.AppendLine(("CUE {0} at {1}:{2:00}" -f ($i + 1), [int][math]::Floor($s / 60), ($s % 60)))
    [void]$srt.AppendLine()
}
[System.IO.File]::WriteAllText((Join-Path $out "sleep-test.srt"), $srt.ToString(), (New-Object System.Text.UTF8Encoding($false)))

# PCM tracks (mkv carries PCM): 24-bit, 32-bit float, 5.1 16-bit
$vid = @("-f","lavfi","-i","testsrc2=size=640x360:rate=30:duration=30")
$tone = @("-f","lavfi","-i","sine=frequency=440:sample_rate=48000:duration=30")
Run "pcm24.mkv"  ($vid + $tone + @("-af","volume=0.15","-c:v","libx264","-preset","veryfast","-pix_fmt","yuv420p","-c:a","pcm_s24le","-shortest"))
Run "pcmf32.mkv" ($vid + $tone + @("-af","volume=0.15","-c:v","libx264","-preset","veryfast","-pix_fmt","yuv420p","-c:a","pcm_f32le","-shortest"))
Run "pcm51.mkv"  ($vid + $tone + @("-filter_complex","[1:a]volume=0.15,asplit=6[a0][a1][a2][a3][a4][a5];[a0][a1][a2][a3][a4][a5]join=inputs=6:channel_layout=5.1[a]","-map","0:v","-map","[a]","-c:v","libx264","-preset","veryfast","-pix_fmt","yuv420p","-c:a","pcm_s16le","-shortest"))

# HDR10 (HEVC Main10, BT.2020 / PQ, mastering metadata), 1080p, 15 s
Run "hdr10.mp4" @("-f","lavfi","-i","testsrc2=size=1920x1080:rate=30:duration=15","-f","lavfi","-i","sine=frequency=440:sample_rate=48000:duration=15","-af","volume=0.15","-vf","format=yuv420p10le","-c:v","libx265","-preset","ultrafast","-pix_fmt","yuv420p10le","-tag:v","hvc1","-x265-params","colorprim=bt2020:transfer=smpte2084:colormatrix=bt2020nc:master-display=G(13250,34500)B(7500,3000)R(34000,16000)WP(15635,16450)L(10000000,1):max-cll=1000,400:hdr10=1:repeat-headers=1:log-level=error","-c:a","aac","-b:a","96k","-shortest")

# 4K SDR HEVC, 60 s, looped on the phone for the thermal run
Run "uhd-sdr.mp4" @("-f","lavfi","-i","testsrc2=size=3840x2160:rate=30:duration=60","-f","lavfi","-i","sine=frequency=440:sample_rate=48000:duration=60","-af","volume=0.15","-c:v","libx265","-preset","ultrafast","-crf","26","-pix_fmt","yuv420p","-tag:v","hvc1","-x265-params","log-level=error","-c:a","aac","-b:a","96k","-shortest")

# push everything and register it with the media store
foreach ($f in Get-ChildItem $out -File) {
    & $adb push $f.FullName "/sdcard/Movies/AptureTest/$($f.Name)" | Out-Null
    if ($f.Extension -ne ".srt") { & $adb shell am broadcast -a android.intent.action.MEDIA_SCANNER_SCAN_FILE -d "file:///sdcard/Movies/AptureTest/$($f.Name)" | Out-Null }
}
"[{0}] ALL DONE" -f (Get-Date -Format HH:mm:ss)
