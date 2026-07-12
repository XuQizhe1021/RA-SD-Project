[CmdletBinding()]
param(
    [switch]$RemoveData
)

$ErrorActionPreference = "SilentlyContinue"

. (Join-Path $PSScriptRoot "common.ps1")

& (Join-Path $PSScriptRoot "stop_db.ps1") -Quiet
Stop-ManagedProcess -PidFile $script:BackendPidFile

if ($RemoveData) {
    if (Test-Path $script:MySqlDataDir) {
        Get-ChildItem -Path $script:MySqlDataDir -Force -ErrorAction SilentlyContinue | Remove-Item -Recurse -Force -ErrorAction SilentlyContinue
    }

    Remove-Item -Path $script:DbFlagFile -Force -ErrorAction SilentlyContinue
    Remove-Item -Path $script:LogDir -Recurse -Force -ErrorAction SilentlyContinue
}
