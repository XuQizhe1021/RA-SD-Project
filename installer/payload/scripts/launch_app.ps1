[CmdletBinding()]
param(
    [switch]$SkipBrowser
)

$ErrorActionPreference = "Stop"

. (Join-Path $PSScriptRoot "common.ps1")

Write-Step "Launch HQ Training Management System"
Ensure-BaseDirectories

if (-not (Test-Path $script:BackendJarFile)) {
    throw "Backend jar was not found in the installation directory."
}

if (-not (Test-Path $script:AppConfigFile)) {
    throw "application-prod.yml was not found in the installation directory."
}

if (-not (Test-Path $script:DbFlagFile)) {
    & (Join-Path $PSScriptRoot "init_db.ps1")
}

& (Join-Path $PSScriptRoot "start_db.ps1") -Quiet

if (Test-TcpPort -HostName "127.0.0.1" -Port $script:ServerPort) {
    Write-Info "Backend is already running on port $($script:ServerPort)."
}
else {
    $javaExe = Get-ShortPath -Path (Get-JavaExecutable)
    $backendJarPath = Get-ShortPath -Path $script:BackendJarFile
    $appConfigPath = Get-ShortPath -Path $script:AppConfigFile
    $javaArguments = @(
        "-jar",
        """$backendJarPath""",
        "--spring.profiles.active=prod",
        "--spring.config.additional-location=""$((New-FileLocationArgument -Path $appConfigPath))"""
    )

    $stdoutLog = Join-Path $script:LogDir "backend.out.log"
    $stderrLog = Join-Path $script:LogDir "backend.err.log"

    Start-LoggedProcess `
        -FilePath $javaExe `
        -ArgumentList $javaArguments `
        -WorkingDirectory $script:InstallRoot `
        -PidFile $script:BackendPidFile `
        -StdOutFile $stdoutLog `
        -StdErrFile $stderrLog | Out-Null

    if (-not (Wait-HttpReady -Url $script:ApplicationUrl -MaxAttempts 40 -DelaySeconds 2)) {
        Stop-ManagedProcess -PidFile $script:BackendPidFile
        throw "Backend did not become ready in time."
    }
}

Write-Info "Application URL: $($script:ApplicationUrl)"
if (-not $SkipBrowser) {
    Start-Process $script:ApplicationUrl
}
