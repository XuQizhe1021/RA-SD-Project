[CmdletBinding()]
param(
    [switch]$ForceReinitialize
)

$ErrorActionPreference = "Stop"

. (Join-Path $PSScriptRoot "common.ps1")

Write-Step "Initialize private MySQL data"
Ensure-BaseDirectories

$mysqlSystemDir = Join-Path $script:MySqlDataDir "mysql"
$hasExistingData = Test-Path $mysqlSystemDir

if ($ForceReinitialize) {
    Write-Warn "Force reinitialize was requested. Existing private database data will be removed."
    & (Join-Path $PSScriptRoot "stop_db.ps1") -Quiet
    if (Test-Path $script:MySqlDataDir) {
        Get-ChildItem -Path $script:MySqlDataDir -Force -ErrorAction SilentlyContinue | Remove-Item -Recurse -Force -ErrorAction SilentlyContinue
    }
    Remove-Item -Path $script:DbFlagFile -Force -ErrorAction SilentlyContinue
    $hasExistingData = $false
}

if ((Test-Path $script:DbFlagFile) -and $hasExistingData) {
    Write-Info "Private database is already initialized."
    exit 0
}

if ($hasExistingData -and -not (Test-Path $script:DbFlagFile)) {
    throw "Private database data exists, but initialization flag is missing. Delete the data directory or rerun with -ForceReinitialize."
}

$mysqldExe = Get-MySqlExecutable -ExecutableName "mysqld.exe"
$defaultsFilePath = Get-ShortPath -Path $script:MySqlDefaultsFile
$baseDirPath = Get-ShortPath -Path $script:MySqlRoot
$dataDirPath = Get-ShortPath -Path $script:MySqlDataDir
Write-Info "Creating empty MySQL data directory..."

& $mysqldExe `
    "--defaults-file=$defaultsFilePath" `
    "--basedir=$baseDirPath" `
    "--datadir=$dataDirPath" `
    "--initialize-insecure" `
    "--console"

if ($LASTEXITCODE -ne 0) {
    throw "MySQL data directory initialization failed."
}

& (Join-Path $PSScriptRoot "start_db.ps1") -Quiet

$bootstrapSql = @"
ALTER USER IF EXISTS 'root'@'localhost' IDENTIFIED BY '$($script:DbRootPassword)';
CREATE USER IF NOT EXISTS 'root'@'127.0.0.1' IDENTIFIED BY '$($script:DbRootPassword)';
GRANT ALL PRIVILEGES ON *.* TO 'root'@'127.0.0.1' WITH GRANT OPTION;
FLUSH PRIVILEGES;
"@

Write-Info "Configuring MySQL accounts..."
Invoke-MySqlCommand -Sql $bootstrapSql -UserName "root" -Password "" -HostName "localhost"

Write-Info "Importing business schema and seed data..."
Invoke-MySqlSqlFile -SqlFilePath $script:DbInitSqlFile -UserName "root" -Password $script:DbRootPassword

$grantSql = @"
CREATE USER IF NOT EXISTS '$($script:DbAppUser)'@'127.0.0.1' IDENTIFIED BY '$($script:DbAppPassword)';
GRANT ALL PRIVILEGES ON $($script:DbName).* TO '$($script:DbAppUser)'@'127.0.0.1';
FLUSH PRIVILEGES;
"@

Invoke-MySqlCommand -Sql $grantSql -UserName "root" -Password $script:DbRootPassword

Set-Content -Path $script:DbFlagFile -Value (Get-Date -Format "yyyy-MM-dd HH:mm:ss") -Encoding ASCII
Write-Info "Private database initialization completed."
