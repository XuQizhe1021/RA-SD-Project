[CmdletBinding()]
param(
    [switch]$Quiet
)

$ErrorActionPreference = "Stop"

. (Join-Path $PSScriptRoot "common.ps1")

if (-not $Quiet) {
    Write-Step "Start private MySQL runtime"
}

Ensure-BaseDirectories

if (Test-TcpPort -HostName $script:DbHost -Port $script:DbPort) {
    if (-not $Quiet) {
        Write-Info "MySQL is already running on port $($script:DbPort)."
    }
    exit 0
}

$mysqldExe = Get-MySqlExecutable -ExecutableName "mysqld.exe"
$defaultsFilePath = Get-ShortPath -Path $script:MySqlDefaultsFile
$baseDirPath = Get-ShortPath -Path $script:MySqlRoot
$dataDirPath = Get-ShortPath -Path $script:MySqlDataDir
$workingDirectory = Get-ShortPath -Path $script:MySqlRoot
$arguments = @(
    "--defaults-file=$defaultsFilePath",
    "--basedir=$baseDirPath",
    "--datadir=$dataDirPath",
    "--port=$script:DbPort",
    "--bind-address=$script:DbHost",
    "--console"
)

$stdoutLog = Join-Path $script:LogDir "mysql.out.log"
$stderrLog = Join-Path $script:LogDir "mysql.err.log"

Start-LoggedProcess `
    -FilePath $mysqldExe `
    -ArgumentList $arguments `
    -WorkingDirectory $workingDirectory `
    -PidFile $script:DbPidFile `
    -StdOutFile $stdoutLog `
    -StdErrFile $stderrLog | Out-Null

if (-not (Wait-TcpPort -HostName $script:DbHost -Port $script:DbPort -MaxAttempts 40 -DelaySeconds 2)) {
    Stop-ManagedProcess -PidFile $script:DbPidFile
    throw "MySQL did not become ready in time."
}

if (-not $Quiet) {
    Write-Info "MySQL is ready on port $($script:DbPort)."
}
