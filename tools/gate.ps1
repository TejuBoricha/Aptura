# ==============================================================================
# Aptura Local Quality Gate (Fail-Fast Verification)
# Runs:
#   1. Unit Test Suite (compiles debug + unit tests, enforces timeout & fail-fast execution)
#   2. Android Lint (Validates opt-in annotations, licenses & offline invariants)
#   3. Release APK + App Bundle builds, which run verifyReleaseApk & verifyReleaseBundle. By default they run only
#      when the branch changes something that can alter the release build (tools/gate/release_scope.py decides, and
#      runs them whenever it is unsure). -Full always runs them, -Quick never does. CI runs them on every pull request.
#   4. Agent coordination checker tests (tools/agents), when Python 3 is installed; CI always runs them
#   5. Secret & workflow pin guards (tools/security): fails on tracked secrets or unpinned workflow actions
#   6. Release-scope tests (tools/gate) and release smoke script tests (tools/smoke)
#   7. A warning (never a failure) for commits that no remote has, from tools/gate/unpushed_commits.py
# ==============================================================================

param(
    [switch]$Quick,
    [switch]$Full
)

$ErrorActionPreference = "Stop"

if ($Quick -and $Full) {
    Write-Host "Use either -Quick or -Full, not both." -ForegroundColor Red
    exit 2
}

# Windows installs Python as `py`, `python` or `python3`; any of them will do. Skipped, not failed, without one,
# because the app itself does not need Python and CI runs these tests on every pull request.
$python = @("py", "python", "python3") | Where-Object { Get-Command $_ -ErrorAction SilentlyContinue } | Select-Object -First 1

# Decides whether the R8 release builds run. Never throws, and any doubt means "run them": skipping is only allowed
# when the diff against origin/main was read successfully and holds no release-affecting path.
function Get-ReleaseBuildDecision {
    if ($Full)  { return @{ Run = $true;  Why = "-Full was given" } }
    if ($Quick) { return @{ Run = $false; Why = "-Quick was given" } }
    try {
        if (-not $python) { return @{ Run = $true; Why = "Python 3 not found, so the change cannot be scoped" } }
        $base = git merge-base HEAD origin/main 2>$null
        if ($LASTEXITCODE -ne 0 -or -not $base) { return @{ Run = $true; Why = "no origin/main to compare with (run git fetch)" } }
        $tracked = @(git -c core.quotepath=off diff --name-only $base 2>$null)
        if ($LASTEXITCODE -ne 0) { return @{ Run = $true; Why = "git diff failed" } }
        $untracked = @(git -c core.quotepath=off ls-files --others --exclude-standard 2>$null)
        if ($LASTEXITCODE -ne 0) { return @{ Run = $true; Why = "git ls-files failed" } }
        $reasons = @(@($tracked + $untracked) | & $python tools/gate/release_scope.py)
        switch ($LASTEXITCODE) {
            1 { return @{ Run = $false; Why = "no release-affecting file changed against origin/main" } }
            0 { return @{ Run = $true;  Why = "release-affecting change: " + (($reasons | Select-Object -First 4) -join "; ") } }
            default { return @{ Run = $true; Why = "could not scope the change (classifier exit code $LASTEXITCODE)" } }
        }
    } catch {
        return @{ Run = $true; Why = "could not scope the change: $_" }
    }
}

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
Write-Host " Aptura Atomic Verification Gate$(if ($Quick) { ' (Quick Mode)' } elseif ($Full) { ' (Full Mode)' } else { '' })" -ForegroundColor Yellow
Write-Host "==========================================================" -ForegroundColor Yellow

Run-Step "Execute Unit Tests (Fail-Fast)" {
    .\gradlew.bat testDebugUnitTest --fail-fast --daemon
}

Run-Step "Execute Android Lint (Debug)" {
    .\gradlew.bat lintDebug --daemon
}

$release = Get-ReleaseBuildDecision
if ($release.Run) {
    Write-Host "`n>>> Release builds will run: $($release.Why)" -ForegroundColor Cyan

    Run-Step "Release APK + shipped-code invariants (R8, no network/telemetry code)" {
        .\gradlew.bat assembleRelease --daemon
    }

    Run-Step "Release App Bundle (.aab) + shipped-code invariants (R8, no network/telemetry code)" {
        .\gradlew.bat bundleRelease --daemon
    }
} else {
    Write-Host "`n>>> [SKIPPED] Release APK/AAB R8 builds: $($release.Why). CI runs them on every pull request; use -Full to run them here." -ForegroundColor Yellow
}

if ($python) {
    Run-Step "Agent coordination checker tests (tools/agents)" {
        & $python -m unittest discover -s tools/agents
    }
    Run-Step "Security & workflow pin guards (tools/security)" {
        & $python -m unittest discover -s tools/security
        if ($LASTEXITCODE -ne 0) { throw "Security guard tests failed" }
        & $python tools/security/check_no_secret_files.py
    }
    Run-Step "Release-scope tests (tools/gate)" {
        & $python -m unittest discover -s tools/gate
    }
    Run-Step "Release smoke script tests (tools/smoke)" {
        & $python -m unittest discover -s tools/smoke
    }
    Run-Step "FFmpeg LGPL guard tests (tools/ffmpeg)" {
        & $python -m unittest discover -s tools/ffmpeg
    }
} else {
    Write-Host "`n>>> [SKIPPED] Agent coordination checker and secret file guard: Python 3 not found (CI runs them)" -ForegroundColor Yellow
}

# Commits that exist only on this machine are lost with it. This is a warning, not a failure: a branch being worked on is
# expected to be ahead of its remote. It is deliberately not a Run-Step, which would stop the gate on a non-zero exit.
$unpushedWarning = $false
if ($python) {
    Write-Host "`n>>> [CHECK] Commits that no remote has (warning only)..." -ForegroundColor Cyan
    & $python tools/gate/unpushed_commits.py
    if ($LASTEXITCODE -ne 0) { $unpushedWarning = $true }
}

Write-Host "`n==========================================================" -ForegroundColor Green
Write-Host " ALL QUALITY GATES PASSED (0 Errors, 0 Regressions)" -ForegroundColor Green
Write-Host "==========================================================" -ForegroundColor Green
if ($unpushedWarning) {
    Write-Host " NOTE: some commits exist only on this machine (see the warning above). The gate passed; they are not backed up." -ForegroundColor Yellow
}
exit 0
