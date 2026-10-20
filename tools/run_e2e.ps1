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

# Find adb and emulator
$adb = (Get-Command adb -ErrorAction SilentlyContinue).Source
if (-not $adb) {
    $adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
}
$emulator = (Get-Command emulator -ErrorAction SilentlyContinue).Source
if (-not $emulator) {
    $emulator = "$env:LOCALAPPDATA\Android\Sdk\emulator\emulator.exe"
}

Write-Host "[1/4] Checking connected devices..." -ForegroundColor Yellow
$devices = & $adb devices | Where-Object { $_ -match "\bdevice\b" -and $_ -notmatch "List of" }
$startedEmulator = $false

if (-not $devices) {
    Write-Host "[*] No active emulator detected. Booting Pixel_8_API_35..." -ForegroundColor Yellow
    $emuArgs = @("-avd", "Pixel_8_API_35", "-no-audio", "-no-boot-anim", "-no-snapshot-load")
    $emuProcess = Start-Process -FilePath $emulator -ArgumentList $emuArgs -PassThru
    $startedEmulator = $true

    Write-Host "[*] Waiting for device connection via adb..." -ForegroundColor Yellow
    & $adb wait-for-device

    Write-Host "[*] Waiting for Android OS boot completion (sys.boot_completed=1)..." -ForegroundColor Yellow
    $booted = $false
    $timeoutSeconds = 120
    $startTime = [System.DateTime]::UtcNow

    while (-not $booted) {
        $bootProp = (& $adb shell getprop sys.boot_completed).Trim()
        if ($bootProp -eq "1") {
            $booted = $true
            break
        }
        $elapsed = ([System.DateTime]::UtcNow - $startTime).TotalSeconds
        if ($elapsed -gt $timeoutSeconds) {
            Write-Host "[!] Timed out waiting for emulator boot." -ForegroundColor Red
            if ($startedEmulator) { & $adb emu kill }
            exit 1
        }
        Start-Sleep -Seconds 2
    }
    Write-Host "[*] Emulator booted successfully. Stabilizing launcher (5s)..." -ForegroundColor Green
    Start-Sleep -Seconds 5
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
