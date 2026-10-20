# ==============================================================================
# Aptura Local Quality Gate (Fail-Fast Verification)
# Runs:
#   1. Unit Test Compilation (Catches syntax & visibility errors)
#   2. Unit Test Suite (Enforces 30s timeout & fail-fast execution)
#   3. Android Lint (Validates opt-in annotations, licenses & offline invariants)
# ==============================================================================

$ErrorActionPreference = "Stop"

function Run-Step([string]$Name, [scriptblock]$Command) {
    Write-Host "`n>>> [GATE STEP] $Name..." -ForegroundColor Cyan
    $sw = [System.Diagnostics.Stopwatch]::StartNew()
    try {
        & $Command
        if ($LASTEXITCODE -ne 0) {
            Write-Host ">>> [FAILED] $Name (Exit code: $LASTEXITCODE)" -ForegroundColor Red
            exit $LASTEXITCODE
        }
        $sw.Stop()
        Write-Host ">>> [PASSED] $Name in $([math]::Round($sw.Elapsed.TotalSeconds, 2))s" -ForegroundColor Green
    } catch {
        Write-Host ">>> [ERROR] $Name failed: $_" -ForegroundColor Red
        exit 1
    }
}

Write-Host "==========================================================" -ForegroundColor Yellow
Write-Host " Aptura Atomic Verification Gate" -ForegroundColor Yellow
Write-Host "==========================================================" -ForegroundColor Yellow

Run-Step "Compile Debug & Unit Tests" {
    .\gradlew.bat compileDebugUnitTestKotlin --daemon
}

Run-Step "Execute Unit Tests (Fail-Fast)" {
    .\gradlew.bat testDebugUnitTest --fail-fast --daemon
}

Run-Step "Execute Android Lint (Debug)" {
    .\gradlew.bat lintDebug --daemon
}

Write-Host "`n==========================================================" -ForegroundColor Green
Write-Host " ALL QUALITY GATES PASSED (0 Errors, 0 Regressions)" -ForegroundColor Green
Write-Host "==========================================================" -ForegroundColor Green
exit 0
