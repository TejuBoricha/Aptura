<#
.SYNOPSIS
    Automated Headless On-Device User Journey & Flow Verification Runner.
.DESCRIPTION
    Executes comprehensive Compose UI user journeys headlessly on an active emulator
    or connected device without taking slow manual screenshots.
    
    Validates:
    - Settings navigation & Privacy Policy modal dialog.
    - Honey-Amber and Champagne Gold accent theme swatches.
    - Folder navigation bottom sheet stability against fling boundary oscillation.
    - View Options layout toggling and sheet dismissals.

.EXAMPLE
    .\tools\test-flow.ps1
    .\tools\test-flow.ps1 -All
#>
param(
    [switch]$All,
    [string]$AvdName = "Pixel_8_API_35"
)

$ErrorActionPreference = "Stop"

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host " Aptura Headless User Journey Verification" -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

# 1. Ensure emulator / device is active
$emulatorScript = Join-Path $PSScriptRoot "emulator.ps1"
$adbCheck = & $emulatorScript -Status
$dev = & $emulatorScript -Shell "echo ping" 2>$null

if ($LASTEXITCODE -ne 0 -or -not $dev) {
    Write-Host "[*] No active device detected. Booting emulator $AvdName..." -ForegroundColor Yellow
    & $emulatorScript -Start -AvdName $AvdName
    if ($LASTEXITCODE -ne 0) {
        Write-Error "Failed to boot emulator $AvdName."
        exit 1
    }
} else {
    Write-Host "[OK] Connected device verified." -ForegroundColor Green
}

# 2. Determine target test suite
$testClass = "org.aptura.player.ApturaUserJourneyTest"
$gradleArgs = @("connectedDebugAndroidTest")

if (-not $All) {
    Write-Host "[*] Running targeted user journey suite: $testClass" -ForegroundColor Cyan
    $gradleArgs += "-Pandroid.testInstrumentationRunnerArguments.class=$testClass"
} else {
    Write-Host "[*] Running full connected AndroidTest suite..." -ForegroundColor Cyan
}

# 3. Execute Gradle Task
$sw = [System.Diagnostics.Stopwatch]::StartNew()
& ".\gradlew.bat" @gradleArgs

$exitCode = $LASTEXITCODE
$sw.Stop()

Write-Host ""
if ($exitCode -eq 0) {
    Write-Host "==========================================================" -ForegroundColor Green
    Write-Host " [PASSED] ALL USER JOURNEYS VERIFIED SUCCESSFULLY" -ForegroundColor Green
    Write-Host " Execution Time: $([math]::Round($sw.Elapsed.TotalSeconds, 2))s" -ForegroundColor Green
    Write-Host "==========================================================" -ForegroundColor Green
    exit 0
} else {
    Write-Host "==========================================================" -ForegroundColor Red
    Write-Host " [FAILED] USER JOURNEY TESTS ENCOUNTERED FAILURES" -ForegroundColor Red
    Write-Host " Check HTML report under app/build/reports/androidTests/connected/" -ForegroundColor Yellow
    Write-Host "==========================================================" -ForegroundColor Red
    exit $exitCode
}
