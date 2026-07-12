# Launch the packaged backend with embedded frontend assets.

[CmdletBinding()]
param(
    [string]$DbHost = "127.0.0.1",
    [int]$DbPort = 3306,
    [string]$DbName = "hq_training",
    [string]$DbUser = "root",
    [string]$DbPassword = "123456",
    [int]$ServerPort = 18080,
    [switch]$SkipBrowser,
    [switch]$RebuildDatabase
)

$ErrorActionPreference = "Stop"

$ProjectRoot = $PSScriptRoot
$BackendRoot = Join-Path $ProjectRoot "backend"
$TargetRoot = Join-Path $BackendRoot "target"
$SqlFile = Join-Path $BackendRoot "src\main\resources\db\mysql\001_init.sql"
$ApplicationUrl = "http://127.0.0.1:$ServerPort"

function Write-Step {
    param([string]$Message)
    Write-Host ""
    Write-Host ("=" * 70) -ForegroundColor DarkCyan
    Write-Host $Message -ForegroundColor Cyan
    Write-Host ("=" * 70) -ForegroundColor DarkCyan
}

function Write-Info {
    param([string]$Message)
    Write-Host "[INFO] $Message" -ForegroundColor Green
}

function Test-TcpPort {
    param(
        [string]$HostName,
        [int]$Port
    )

    $client = New-Object System.Net.Sockets.TcpClient
    try {
        $asyncResult = $client.BeginConnect($HostName, $Port, $null, $null)
        if (-not $asyncResult.AsyncWaitHandle.WaitOne(1500, $false)) {
            return $false
        }

        $client.EndConnect($asyncResult)
        return $true
    }
    catch {
        return $false
    }
    finally {
        $client.Dispose()
    }
}

function Get-MySqlClientPath {
    $mysqlCommand = Get-Command mysql.exe -ErrorAction SilentlyContinue
    if ($mysqlCommand) {
        return $mysqlCommand.Source
    }

    return $null
}

function Invoke-MySqlSqlFile {
    param(
        [string]$MySqlExe,
        [string]$SqlFilePath
    )

    $cmdLine = ('"{0}" --default-character-set=utf8mb4 --protocol=tcp -h{1} -P{2} -u{3} < "{4}"' -f $MySqlExe, $DbHost, $DbPort, $DbUser, $SqlFilePath)

    $env:MYSQL_PWD = $DbPassword
    try {
        cmd.exe /c $cmdLine
        if ($LASTEXITCODE -ne 0) {
            throw "Database import failed with exit code $LASTEXITCODE."
        }
    }
    finally {
        Remove-Item Env:MYSQL_PWD -ErrorAction SilentlyContinue
    }
}

function Wait-HttpReady {
    param([string]$Url)

    for ($i = 1; $i -le 30; $i++) {
        try {
            $response = Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec 2
            if ($response.StatusCode -ge 200 -and $response.StatusCode -lt 500) {
                return $true
            }
        }
        catch {
            Start-Sleep -Seconds 2
        }
    }

    return $false
}

Write-Step "Step 1: Check build artifacts"
if (-not (Test-Path $TargetRoot)) {
    throw "backend\\target was not found. Run build_delivery.bat first."
}

$jarFile = Get-ChildItem -Path $TargetRoot -Filter "*.jar" |
    Where-Object { $_.Name -notlike "*.original" } |
    Sort-Object LastWriteTime -Descending |
    Select-Object -First 1

if (-not $jarFile) {
    throw "No executable jar was found. Run build_delivery.bat first."
}

Write-Info "Detected backend jar: $($jarFile.Name)"

Write-Step "Step 2: Check database connectivity"
if (-not (Test-TcpPort -HostName $DbHost -Port $DbPort)) {
    throw "Database port is unreachable: ${DbHost}:${DbPort}. A running MySQL instance is still required at this stage."
}

Write-Info "Database port is reachable: ${DbHost}:${DbPort}"

Write-Step "Step 3: Handle database initialization option"
if ($RebuildDatabase) {
    if (Test-TcpPort -HostName "127.0.0.1" -Port $ServerPort) {
        throw "Backend is already running on port $ServerPort. Stop the current service before rebuilding the database."
    }

    if (-not (Test-Path $SqlFile)) {
        throw "SQL init file was not found: $SqlFile"
    }

    $mysqlExe = Get-MySqlClientPath
    if (-not $mysqlExe) {
        throw "mysql.exe was not found. Install MySQL Client or add the MySQL bin directory to PATH before rebuilding the database."
    }

    Write-Warning "Database rebuild was selected. Existing data in '$DbName' will be replaced by the initialization script."
    Invoke-MySqlSqlFile -MySqlExe $mysqlExe -SqlFilePath $SqlFile
    Write-Info "Database rebuild completed."
}
else {
    Write-Info "Database rebuild was skipped. Existing data will be preserved."
}

Write-Step "Step 4: Start backend if needed"
if (Test-TcpPort -HostName "127.0.0.1" -Port $ServerPort) {
    Write-Info "Backend is already running on port $ServerPort."
}
else {
    $backendCommand = @"
Set-Location '$BackendRoot'
`$env:DB_HOST = '$DbHost'
`$env:DB_PORT = '$DbPort'
`$env:DB_NAME = '$DbName'
`$env:DB_USERNAME = '$DbUser'
`$env:DB_PASSWORD = '$DbPassword'
`$env:SERVER_PORT = '$ServerPort'
Write-Host 'Starting HQ Training Management System...' -ForegroundColor Cyan
java -jar '$($jarFile.FullName)' --spring.profiles.active=prod
"@

    Start-Process -FilePath "powershell.exe" -WorkingDirectory $ProjectRoot -ArgumentList @(
        "-NoExit",
        "-ExecutionPolicy", "Bypass",
        "-Command",
        $backendCommand
    )

    if (-not (Wait-HttpReady -Url $ApplicationUrl)) {
        throw "Backend start was triggered, but the application was not ready in time. Check the new service window for details."
    }

    Write-Info "Backend is ready."
}

Write-Step "Step 5: Open application"
Write-Host "Application URL: $ApplicationUrl" -ForegroundColor Green
if (-not $SkipBrowser) {
    Start-Process $ApplicationUrl
}
