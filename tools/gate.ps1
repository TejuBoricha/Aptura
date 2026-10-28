# ==============================================================================
# Aptura Local Quality Gate (Fail-Fast Verification)
# Runs:
#   1. Unit Test Compilation (Catches syntax & visibility errors)
#   2. Unit Test Suite (Enforces 30s timeout & fail-fast execution)
#   3. Android Lint (Validates opt-in annotations, licenses & offline invariants)
#   4. Release build, which also runs verifyReleaseApk (no network stack / telemetry SDK in the shipped code)
#   5. Agent coordination checker tests (tools/agents), when Python 3 is installed; CI always runs them
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

Run-Step "Release build + shipped-code invariants (R8, no network/telemetry code)" {
    .\gradlew.bat assembleRelease --daemon
}

# Windows installs Python as `py`, `python` or `python3`; any of them will do. Skipped, not failed, without one,
# because the app itself does not need Python and CI runs these tests on every pull request.
$python = @("py", "python", "python3") | Where-Object { Get-Command $_ -ErrorAction SilentlyContinue } | Select-Object -First 1
if ($python) {
    Run-Step "Agent coordination checker tests (tools/agents)" {
        & $python -m unittest discover -s tools/agents
    }
} else {
    Write-Host "`n>>> [SKIPPED] Agent coordination checker tests: Python 3 not found (CI runs them)" -ForegroundColor Yellow
}

Write-Host "`n==========================================================" -ForegroundColor Green
Write-Host " ALL QUALITY GATES PASSED (0 Errors, 0 Regressions)" -ForegroundColor Green
Write-Host "==========================================================" -ForegroundColor Green
exit 0
