<#
.SYNOPSIS
    Aptura Automated Device/Emulator End-to-End Test Runner.
.DESCRIPTION
    Boots Android Virtual Device (Pixel_8_API_35) if not already active,
    waits for system boot completion, runs connectedDebugAndroidTest,
    and reports execution results.
.PARAMETER KeepAlive
    Keep emulator running after tests complete.
#>
param(
    [switch]$KeepAlive = $false
)

$ErrorActionPreference = "Stop"

Write-Host "==================================================" -ForegroundColor Cyan
Write-Host "   Aptura Native E2E Test Suite (UI Automator)    " -ForegroundColor Cyan
Write-Host "==================================================" -ForegroundColor Cyan

# Find adb and emulator using local.properties or fallback
$rootDir = Split-Path -Parent $PSScriptRoot
$localProp = Join-Path $rootDir "local.properties"
$sdkPath = $null

if (Test-Path $localProp) {
    $line = Get-Content $localProp | Where-Object { $_ -match "^sdk\.dir\s*=" } | Select-Object -First 1
    if ($line) {
        $rawPath = ($line -replace "^sdk\.dir\s*=\s*", "").Trim()
        $sdkPath = $rawPath.Replace("\:", ":").Replace("\\", "\")
    }
}
if (-not $sdkPath -or -not (Test-Path $sdkPath)) {
    $sdkPath = "C:\Users\tejas\AndroidDev\sdk"
}

$adb = Join-Path $sdkPath "platform-tools\adb.exe"
$emulator = Join-Path $sdkPath "emulator\emulator.exe"

# Clear any stale lock files before checking
$avdDir = Join-Path "$env:USERPROFILE\.android\avd" "Pixel_8_API_35.avd"
if (Test-Path $avdDir) {
    Get-ChildItem -Path $avdDir -Filter "*.lock" -Recurse -ErrorAction SilentlyContinue | Remove-Item -Recurse -Force -ErrorAction SilentlyContinue
}

Write-Host "[1/4] Checking connected devices..." -ForegroundColor Yellow
$devices = & $adb devices | Where-Object { $_ -match "\bdevice\b" -and $_ -notmatch "List of" }
$startedEmulator = $false

if (-not $devices) {
    Write-Host "[*] No active emulator detected. Booting Pixel_8_API_35..." -ForegroundColor Yellow
    $emuArgs = @("-avd", "Pixel_8_API_35", "-no-audio", "-no-boot-anim", "-no-snapshot-load", "-gpu", "auto")
    
    $psi = New-Object System.Diagnostics.ProcessStartInfo
    $psi.FileName = $emulator
    $psi.Arguments = ($emuArgs -join " ")
    $psi.WorkingDirectory = (Split-Path -Parent $emulator)
    $psi.UseShellExecute = $true
    $psi.WindowStyle = [System.Diagnostics.ProcessWindowStyle]::Minimized
    [System.Diagnostics.Process]::Start($psi) | Out-Null
    $startedEmulator = $true

    Write-Host "[*] Polling for device boot completion (max 90s)..." -ForegroundColor Yellow
    $booted = $false
    $timeoutSeconds = 90
    $startTime = [System.DateTime]::UtcNow

    while (-not $booted) {
        $elapsed = ([System.DateTime]::UtcNow - $startTime).TotalSeconds
        if ($elapsed -gt $timeoutSeconds) {
            Write-Host "[!] Timed out waiting for emulator boot." -ForegroundColor Red
            if ($startedEmulator) { & $adb emu kill 2>$null }
            exit 1
        }
        Start-Sleep -Seconds 3
        $bootProp = (& $adb shell getprop sys.boot_completed 2>$null)
        if ($bootProp -and $bootProp.Trim() -eq "1") {
            $booted = $true
            break
        }
    }
    Write-Host "[*] Emulator booted successfully. Stabilizing launcher (3s)..." -ForegroundColor Green
    Start-Sleep -Seconds 3
} else {
    Write-Host "[*] Using already connected device/emulator: $devices" -ForegroundColor Green
}

Write-Host "[2/4] Waking screen and unlocking keyguard..." -ForegroundColor Yellow
& $adb shell input keyevent 82 2>$null

Write-Host "[3/4] Running connectedDebugAndroidTest..." -ForegroundColor Cyan
$gradleCmd = ".\gradlew.bat"
$testArgs = @("connectedDebugAndroidTest", "--info")

$exitCode = 0
try {
    & $gradleCmd @testArgs
    $exitCode = $LASTEXITCODE
} catch {
    $exitCode = 1
}

Write-Host "[4/4] Test execution finished with exit code: $exitCode" -ForegroundColor $(if ($exitCode -eq 0) { "Green" } else { "Red" })

if ($startedEmulator -and -not $KeepAlive) {
    Write-Host "[*] Shutting down automated emulator..." -ForegroundColor Yellow
    & $adb emu kill 2>$null
}

if ($exitCode -ne 0) {
    Write-Host "[!] E2E Tests Failed!" -ForegroundColor Red
    exit $exitCode
} else {
    Write-Host "[OK] All E2E Tests Passed on Device!" -ForegroundColor Green
    exit 0
}
