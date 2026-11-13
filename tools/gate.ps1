# ==============================================================================
# Aptura Local Quality Gate (Fail-Fast Verification)
# Runs:
#   1. Unit Test Compilation (Catches syntax & visibility errors)
#   2. Unit Test Suite (Enforces 30s timeout & fail-fast execution)
#   3. Android Lint (Validates opt-in annotations, licenses & offline invariants)
#   4. Release APK + App Bundle builds, which also run verifyReleaseApk & verifyReleaseBundle (no network stack / telemetry SDK in the shipped code)
#   5. Agent coordination checker tests (tools/agents), when Python 3 is installed; CI always runs them
#   6. Secret file guard (tools/security): fails if a signing key or .env file is tracked; same condition as step 5
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

Run-Step "Release APK + App Bundle (.aab) + shipped-code invariants (R8, no network/telemetry code)" {
    .\gradlew.bat assembleRelease --daemon
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
    .\gradlew.bat bundleRelease --daemon
}

# Windows installs Python as `py`, `python` or `python3`; any of them will do. Skipped, not failed, without one,
# because the app itself does not need Python and CI runs these tests on every pull request.
$python = @("py", "python", "python3") | Where-Object { Get-Command $_ -ErrorAction SilentlyContinue } | Select-Object -First 1
if ($python) {
    Run-Step "Agent coordination checker tests (tools/agents)" {
        & $python -m unittest discover -s tools/agents
    }
    Run-Step "Secret file guard (tools/security)" {
        & $python -m unittest discover -s tools/security
        if ($LASTEXITCODE -ne 0) { throw "Secret file guard tests failed" }
        & $python tools/security/check_no_secret_files.py
    }
} else {
    Write-Host "`n>>> [SKIPPED] Agent coordination checker and secret file guard: Python 3 not found (CI runs them)" -ForegroundColor Yellow
}

Write-Host "`n==========================================================" -ForegroundColor Green
Write-Host " ALL QUALITY GATES PASSED (0 Errors, 0 Regressions)" -ForegroundColor Green
Write-Host "==========================================================" -ForegroundColor Green
exit 0
