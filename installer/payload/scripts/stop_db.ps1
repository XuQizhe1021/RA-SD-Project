[CmdletBinding()]
param(
    [switch]$Quiet,
    [switch]$BootstrapMode
)

$ErrorActionPreference = "Stop"

. (Join-Path $PSScriptRoot "common.ps1")

if (-not $Quiet) {
    Write-Step "Stop private MySQL runtime"
}

if (-not (Test-TcpPort -HostName $script:DbHost -Port $script:DbPort)) {
    Stop-ManagedProcess -PidFile $script:DbPidFile
    if (-not $Quiet) {
        Write-Info "MySQL is not running."
    }
    exit 0
}

try {
    $password = if ($BootstrapMode) { "" } else { $script:DbRootPassword }
    Invoke-MySqlAdminShutdown -UserName "root" -Password $password
}
catch {
    Stop-ManagedProcess -PidFile $script:DbPidFile
}

if (-not (Wait-PortClosed -HostName $script:DbHost -Port $script:DbPort -MaxAttempts 20 -DelaySeconds 2)) {
    Stop-ManagedProcess -PidFile $script:DbPidFile
}

if (Test-TcpPort -HostName $script:DbHost -Port $script:DbPort) {
    throw "Failed to stop MySQL."
}

Remove-Item -Path $script:DbPidFile -Force -ErrorAction SilentlyContinue

if (-not $Quiet) {
    Write-Info "MySQL has stopped."
}
