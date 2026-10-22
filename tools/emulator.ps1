<#
.SYNOPSIS
    Permanent, Resilient Android Emulator & Device Manager for Aptura.
.DESCRIPTION
    Guarantees non-blocking lifecycle management for the Android Emulator on Windows:
    1. Resolves SDK path dynamically from local.properties.
    2. Clears stale AVD lock files (hardware-qemu.ini.lock, multiinstance.lock) to prevent boot hangs.
    3. Manages ADB daemon cleanly, killing zombie processes if port 5037 stalls.
    4. Starts emulator via Start-Process completely detached from PowerShell standard I/O pipes.
    5. Polls boot status with strict bounded timeouts (never blocks indefinitely).
    6. Provides reliable screenshot captures and app launch utilities.
.EXAMPLE
    .\tools\emulator.ps1 -Start
    .\tools\emulator.ps1 -Status
    .\tools\emulator.ps1 -Capture -Path "review/captures/live_screen.png"
    .\tools\emulator.ps1 -Stop
#>
param(
    [switch]$Start,
    [switch]$Stop,
    [switch]$Restart,
    [switch]$Status,
    [switch]$Capture,
    [string]$Path = "review/captures/screen.png",
    [switch]$InstallAndRun,
    [string]$Shell = "",
    [string]$AvdName = "Pixel_8_API_35",
    [int]$BootTimeoutSec = 90
)

$ErrorActionPreference = "Stop"

# 1. Resolve Android SDK Path
function Get-ApturaSdkPath {
    $scriptDir = $PSScriptRoot
    if (-not $scriptDir) { $scriptDir = (Get-Location).Path }
    $rootDir = Split-Path -Parent $scriptDir
    $localProp = Join-Path $rootDir "local.properties"
    
    if (Test-Path $localProp) {
        $line = Get-Content $localProp | Where-Object { $_ -match "^sdk\.dir\s*=" } | Select-Object -First 1
        if ($line) {
            $rawPath = ($line -replace "^sdk\.dir\s*=\s*", "").Trim()
            $sdkPath = $rawPath.Replace("\:", ":").Replace("\\", "\")
            if (Test-Path $sdkPath) {
                return $sdkPath
            }
        }
    }
    
    $fallbacks = @(
        "C:\Users\tejas\AndroidDev\sdk",
        "$env:LOCALAPPDATA\Android\Sdk",
        "$env:ANDROID_HOME"
    )
    foreach ($fb in $fallbacks) {
        if ($fb -and (Test-Path $fb)) { return $fb }
    }
    throw "Android SDK path could not be resolved from local.properties or common paths."
}

$SdkPath = Get-ApturaSdkPath
$AdbExe = Join-Path $SdkPath "platform-tools\adb.exe"
$EmulatorExe = Join-Path $SdkPath "emulator\emulator.exe"

if (-not (Test-Path $AdbExe)) { throw "adb.exe not found at $AdbExe" }
if (-not (Test-Path $EmulatorExe)) { throw "emulator.exe not found at $EmulatorExe" }

# 2. Resilient ADB Invoker with Strict Timeout
function Invoke-AdbWithTimeout {
    param(
        [string[]]$AdbArgs,
        [int]$TimeoutSec = 10
    )
    
    $pinfo = New-Object System.Diagnostics.ProcessStartInfo
    $pinfo.FileName = $AdbExe
    $pinfo.Arguments = ($AdbArgs -join " ")
    $pinfo.RedirectStandardOutput = $true
    $pinfo.RedirectStandardError = $true
    $pinfo.UseShellExecute = $false
    $pinfo.CreateNoWindow = $true

    $p = New-Object System.Diagnostics.Process
    $p.StartInfo = $pinfo

    try {
        [void]$p.Start()
        $stdoutTask = $p.StandardOutput.ReadToEndAsync()
        $stderrTask = $p.StandardError.ReadToEndAsync()

        if (-not $p.WaitForExit($TimeoutSec * 1000)) {
            try { $p.Kill() } catch {}
            Write-Warning "ADB command '$($AdbArgs -join ' ')' timed out after ${TimeoutSec}s and was terminated."
            return [PSCustomObject]@{ ExitCode = -1; Stdout = ""; Stderr = "TIMEOUT" }
        }

        $stdout = $stdoutTask.Result
        $stderr = $stderrTask.Result
        return [PSCustomObject]@{ ExitCode = $p.ExitCode; Stdout = $stdout; Stderr = $stderr }
    } catch {
        return [PSCustomObject]@{ ExitCode = -2; Stdout = ""; Stderr = $_.Exception.Message }
    } finally {
        $p.Dispose()
    }
}

# 3. Clean Stale AVD Lock Files
function Clear-AvdLocks {
    param([string]$Avd)
    $avdDir = Join-Path "$env:USERPROFILE\.android\avd" "$Avd.avd"
    if (Test-Path $avdDir) {
        $locks = Get-ChildItem -Path $avdDir -Filter "*.lock" -Recurse -ErrorAction SilentlyContinue
        if ($locks) {
            Write-Host "[*] Purging $($locks.Count) stale lock file(s) from $avdDir..." -ForegroundColor Yellow
            foreach ($lock in $locks) {
                Remove-Item -Path $lock.FullName -Recurse -Force -ErrorAction SilentlyContinue
            }
        }
    }
}

# 4. Check If Active Device Exists
function Get-ActiveDevice {
    for ($i = 0; $i -lt 3; $i++) {
        $res = Invoke-AdbWithTimeout -AdbArgs @("devices") -TimeoutSec 10
        if ($res.ExitCode -eq 0) {
            $lines = @($res.Stdout -split "`r?`n" | Where-Object { $_ -match "\bdevice\b" -and $_ -notmatch "List of" })
            if ($lines.Count -gt 0) {
                $devId = ($lines[0] -split "\s+")[0]
                return $devId
            }
        }
        Start-Sleep -Milliseconds 800
    }
    return $null
}

# 5. Start Emulator
function Start-ApturaEmulator {
    param([string]$Avd, [int]$TimeoutSec)

    $existing = Get-ActiveDevice
    if ($existing) {
        Write-Host "[OK] Emulator/device is already online and ready: $existing" -ForegroundColor Green
        return $existing
    }

    Write-Host "[1/4] Cleaning stale AVD locks and verifying ADB daemon..." -ForegroundColor Cyan
    Clear-AvdLocks -Avd $Avd

    # Ensure ADB server is responsive
    $adbCheck = Invoke-AdbWithTimeout -AdbArgs @("devices") -TimeoutSec 5
    if ($adbCheck.ExitCode -ne 0) {
        Write-Host "[*] ADB server unresponsive, resetting..." -ForegroundColor Yellow
        Get-Process -Name "adb" -ErrorAction SilentlyContinue | Stop-Process -Force -ErrorAction SilentlyContinue
        Start-Sleep -Seconds 1
        & $AdbExe start-server
    }

    Write-Host "[2/4] Spawning $Avd via decoupled WMI process..." -ForegroundColor Cyan
    $emuArgs = "-avd $Avd -no-audio -no-boot-anim -no-snapshot-load -gpu auto"
    $cmdLine = "`"$EmulatorExe`" $emuArgs"

    # CRITICAL: Win32_Process::Create spawns under WmiPrvSE.exe outside any IDE/PowerShell runner Job Object
    $res = Invoke-CimMethod -ClassName Win32_Process -MethodName Create -Arguments @{ CommandLine = $cmdLine }
    if ($res.ReturnValue -ne 0) {
        throw "Failed to launch emulator via WMI. ReturnValue: $($res.ReturnValue)"
    }
    Write-Host "[*] Emulator launched successfully (PID: $($res.ProcessId))." -ForegroundColor Green

    Write-Host "[3/4] Polling for Android OS boot completion (max ${TimeoutSec}s)..." -ForegroundColor Cyan
    $startTime = [System.DateTime]::UtcNow
    $booted = $false

    while (-not $booted) {
        $elapsed = ([System.DateTime]::UtcNow - $startTime).TotalSeconds
        if ($elapsed -gt $TimeoutSec) {
            throw "Timed out waiting for emulator $Avd to complete boot after ${TimeoutSec}s."
        }

        Start-Sleep -Seconds 3

        $dev = Get-ActiveDevice
        if ($dev) {
            $bootCheck = Invoke-AdbWithTimeout -AdbArgs @("-s", $dev, "shell", "getprop", "sys.boot_completed") -TimeoutSec 5
            if ($bootCheck.Stdout.Trim() -eq "1") {
                $booted = $true
                break
            }
        }
        Write-Host "  ... waiting for boot completion ($([math]::Round($elapsed))s elapsed)" -ForegroundColor Gray
    }

    $activeDev = Get-ActiveDevice
    Write-Host "[4/4] Waking screen and unlocking keyguard on $activeDev..." -ForegroundColor Cyan
    Invoke-AdbWithTimeout -AdbArgs @("-s", $activeDev, "shell", "input", "keyevent", "82") -TimeoutSec 5 | Out-Null
    Start-Sleep -Seconds 2

    Write-Host "[OK] Emulator $Avd is fully booted, unlocked, and responsive." -ForegroundColor Green
    return $activeDev
}

# 6. Stop Emulator
function Stop-ApturaEmulator {
    param([string]$Avd)
    Write-Host "[*] Shutting down active emulator..." -ForegroundColor Yellow
    $dev = Get-ActiveDevice
    if ($dev) {
        Invoke-AdbWithTimeout -AdbArgs @("-s", $dev, "emu", "kill") -TimeoutSec 5 | Out-Null
        Start-Sleep -Seconds 2
    }
    
    # Clean up any lingering QEMU or emulator processes if kill did not finish
    Get-Process -Name "qemu-system-x86_64", "emulator" -ErrorAction SilentlyContinue | Stop-Process -Force -ErrorAction SilentlyContinue
    Start-Sleep -Seconds 1
    Clear-AvdLocks -Avd $Avd
    Write-Host "[OK] Emulator stopped and lock files cleared." -ForegroundColor Green
}

# 7. Capture Screenshot
function Capture-Screen {
    param([string]$OutputPath)
    $dev = Get-ActiveDevice
    if (-not $dev) {
        throw "No active device found to capture screen."
    }

    $parentDir = Split-Path -Parent $OutputPath
    if ($parentDir -and -not (Test-Path $parentDir)) {
        New-Item -ItemType Directory -Path $parentDir -Force | Out-Null
    }

    $remotePath = "/sdcard/window_dump.png"
    Invoke-AdbWithTimeout -AdbArgs @("-s", $dev, "shell", "screencap", "-p", $remotePath) -TimeoutSec 10 | Out-Null
    Invoke-AdbWithTimeout -AdbArgs @("-s", $dev, "pull", $remotePath, $OutputPath) -TimeoutSec 10 | Out-Null
    Invoke-AdbWithTimeout -AdbArgs @("-s", $dev, "shell", "rm", "-f", $remotePath) -TimeoutSec 5 | Out-Null

    if (Test-Path $OutputPath) {
        $len = (Get-Item $OutputPath).Length
        Write-Host "[OK] Screenshot captured successfully: $OutputPath ($len bytes)" -ForegroundColor Green
    } else {
        throw "Screenshot failed to pull to $OutputPath"
    }
}

# 8. Check Status
function Show-Status {
    param([string]$Avd)
    Write-Host "=== Aptura Emulator & Device Status ===" -ForegroundColor Cyan
    Write-Host "SDK Path:      $SdkPath"
    Write-Host "AVD Name:      $Avd"
    
    $dev = Get-ActiveDevice
    if ($dev) {
        $bootProp = (Invoke-AdbWithTimeout -AdbArgs @("-s", $dev, "shell", "getprop", "sys.boot_completed") -TimeoutSec 5).Stdout.Trim()
        Write-Host "Device Status: ONLINE ($dev)" -ForegroundColor Green
        Write-Host "Boot State:    sys.boot_completed=$bootProp" -ForegroundColor $(if ($bootProp -eq "1") { "Green" } else { "Yellow" })
    } else {
        Write-Host "Device Status: OFFLINE (No active device detected)" -ForegroundColor Yellow
    }

    $qemu = Get-Process -Name "qemu-system-x86_64" -ErrorAction SilentlyContinue
    Write-Host "QEMU Process:  $($(if ($qemu) { "Running (PID: $($qemu.Id))" } else { "None" }))"
}

# --- Main Dispatcher ---
if ($Status) {
    Show-Status -Avd $AvdName
    exit 0
}

if ($Stop) {
    Stop-ApturaEmulator -Avd $AvdName
    exit 0
}

if ($Restart) {
    Stop-ApturaEmulator -Avd $AvdName
    Start-ApturaEmulator -Avd $AvdName -TimeoutSec $BootTimeoutSec
    exit 0
}

if ($Start) {
    Start-ApturaEmulator -Avd $AvdName -TimeoutSec $BootTimeoutSec
    exit 0
}

if ($Capture) {
    Capture-Screen -OutputPath $Path
    exit 0
}

if ($InstallAndRun) {
    $dev = Start-ApturaEmulator -Avd $AvdName -TimeoutSec $BootTimeoutSec
    Write-Host "[*] Building and installing debug APK..." -ForegroundColor Cyan
    & ".\gradlew.bat" "installDebug"
    if ($LASTEXITCODE -ne 0) { throw "Gradle installDebug failed with exit code $LASTEXITCODE" }
    
    Write-Host "[*] Launching Aptura MainActivity..." -ForegroundColor Cyan
    Invoke-AdbWithTimeout -AdbArgs @("-s", $dev, "shell", "am", "start", "-n", "org.aptura.player/.MainActivity") -TimeoutSec 10 | Out-Null
    Start-Sleep -Seconds 3
}

if ($Shell) {
    $dev = Get-ActiveDevice
    if (-not $dev) { throw "No active device found." }
    $cmdParts = $Shell -split "\s+"
    $adbArgs = @("-s", $dev, "shell") + $cmdParts
    $res = Invoke-AdbWithTimeout -AdbArgs $adbArgs -TimeoutSec 15
    if ($res.Stdout) { Write-Host $res.Stdout.Trim() }
    if ($res.Stderr) { Write-Warning $res.Stderr.Trim() }
    exit $res.ExitCode
}

# Default: Show help/status
Show-Status -Avd $AvdName
