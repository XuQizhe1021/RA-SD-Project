# Project startup script.
# Execution order:
# 1. Check MySQL installation
# 2. Start database service
# 3. Import seed database
# 4. Open a new terminal for backend
# 5. Open a new terminal for frontend

[CmdletBinding()]
param(
    [string]$DbHost = "127.0.0.1",
    [int]$DbPort = 3306,
    [string]$DbName = "hq_training",
    [string]$DbUser = "root",
    [string]$DbPassword = "123456",
    [switch]$RebuildDatabase
)

$ErrorActionPreference = "Stop"

$ProjectRoot = $PSScriptRoot
$SqlFile = Join-Path $ProjectRoot "backend\src\main\resources\db\mysql\001_init.sql"

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

function Write-Warn {
    param([string]$Message)
    Write-Host "[WARN] $Message" -ForegroundColor Yellow
}

function Write-Fail {
    param([string]$Message)
    Write-Host "[ERROR] $Message" -ForegroundColor Red
}

function Test-Administrator {
    $currentIdentity = [Security.Principal.WindowsIdentity]::GetCurrent()
    $principal = New-Object Security.Principal.WindowsPrincipal($currentIdentity)
    return $principal.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)
}

function Get-ServiceExecutablePath {
    param([string]$ServiceName)

    $serviceInfo = Get-CimInstance Win32_Service -Filter "Name='$ServiceName'"
    if (-not $serviceInfo -or [string]::IsNullOrWhiteSpace($serviceInfo.PathName)) {
        return $null
    }

    $pathName = $serviceInfo.PathName.Trim()
    if ($pathName.StartsWith('"')) {
        return $pathName.Split('"')[1]
    }

    return $pathName.Split(" ")[0]
}

function Get-SystemMySqlService {
    $serviceCandidates = @("MySQL80", "MySQL", "mysql")
    foreach ($serviceName in $serviceCandidates) {
        $service = Get-Service -Name $serviceName -ErrorAction SilentlyContinue
        if ($service) {
            return $service
        }
    }

    $mysqlServices = Get-Service | Where-Object {
        $_.Name -match "mysql" -or $_.DisplayName -match "mysql"
    } | Sort-Object Name

    return $mysqlServices | Select-Object -First 1
}

function Get-MySqlClientPath {
    param([string]$ServiceName)

    $mysqlCommand = Get-Command mysql.exe -ErrorAction SilentlyContinue
    if ($mysqlCommand) {
        return $mysqlCommand.Source
    }

    $mysqldPath = Get-ServiceExecutablePath -ServiceName $ServiceName
    if (-not $mysqldPath) {
        return $null
    }

    $binDir = Split-Path -Parent $mysqldPath
    $mysqlExe = Join-Path $binDir "mysql.exe"
    if (Test-Path $mysqlExe) {
        return $mysqlExe
    }

    return $null
}

function Test-CommandExists {
    param([string]$CommandName)
    return [bool](Get-Command $CommandName -ErrorAction SilentlyContinue)
}

function Start-MySqlServiceBySc {
    param([string]$ServiceName)

    $startOutput = & sc.exe start $ServiceName 2>&1
    $joinedOutput = ($startOutput | Out-String)

    if ($LASTEXITCODE -ne 0 -and $joinedOutput -notmatch "SERVICE_ALREADY_RUNNING") {
        throw "sc.exe failed to start the service. Output: $joinedOutput"
    }
}

function Invoke-MySqlCommand {
    param(
        [string]$MySqlExe,
        [string[]]$Arguments
    )

    $env:MYSQL_PWD = $DbPassword
    try {
        & $MySqlExe @Arguments
    }
    finally {
        Remove-Item Env:MYSQL_PWD -ErrorAction SilentlyContinue
    }
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

function Wait-MySqlReady {
    param([string]$MySqlExe)

    for ($i = 1; $i -le 20; $i++) {
        try {
            $null = Invoke-MySqlCommand -MySqlExe $MySqlExe -Arguments @(
                "--protocol=tcp",
                "-h$DbHost",
                "-P$DbPort",
                "-u$DbUser",
                "--connect-timeout=2",
                "-e",
                "SELECT 1;"
            ) 2>$null
            return $true
        }
        catch {
            Start-Sleep -Seconds 2
        }
    }

    return $false
}

if (-not (Test-Administrator)) {
    Write-Warn "Current PowerShell session is not elevated. Requesting administrator permission..."

    $elevatedArgs = @(
        "-NoExit",
        "-ExecutionPolicy", "Bypass",
        "-File", $PSCommandPath,
        "-DbHost", $DbHost,
        "-DbPort", $DbPort.ToString(),
        "-DbName", $DbName,
        "-DbUser", $DbUser,
        "-DbPassword", $DbPassword
    )

    if ($RebuildDatabase) {
        $elevatedArgs += "-RebuildDatabase"
    }

    Start-Process -FilePath "powershell.exe" -Verb RunAs -WorkingDirectory $ProjectRoot -ArgumentList $elevatedArgs
    exit 0
}

if (-not (Test-Path $SqlFile)) {
    Write-Fail "SQL init file not found: $SqlFile"
    Read-Host "Press Enter to exit"
    exit 1
}

Write-Step "Step 1: Check local runtime tools"

foreach ($commandName in @("java", "mvn", "node", "npm")) {
    if (-not (Test-CommandExists -CommandName $commandName)) {
        Write-Fail "Command not found: $commandName"
        Write-Host "Please install the missing tool and make sure it is available in PATH." -ForegroundColor Yellow
        Read-Host "Press Enter to exit"
        exit 1
    }
}

Write-Info "Java, Maven, Node.js and npm are available."

Write-Step "Step 2: Detect MySQL service"

$mysqlService = Get-SystemMySqlService
if (-not $mysqlService) {
    Write-Fail "No system-level MySQL service was found."
    Write-Host "Please install MySQL Server as a Windows service, usually named MySQL or MySQL80." -ForegroundColor Yellow
    Read-Host "Press Enter to exit"
    exit 1
}

$mysqlExe = Get-MySqlClientPath -ServiceName $mysqlService.Name
if (-not $mysqlExe) {
    Write-Fail "MySQL service '$($mysqlService.Name)' was found, but mysql.exe client was not found."
    Write-Host "Please install MySQL Client or add the MySQL bin directory to PATH." -ForegroundColor Yellow
    Read-Host "Press Enter to exit"
    exit 1
}

Write-Info "Detected MySQL service: $($mysqlService.Name)"
Write-Info "Detected mysql.exe client: $mysqlExe"

Write-Step "Step 3: Start MySQL service"

if ($mysqlService.Status -ne "Running") {
    Write-Info "Starting MySQL service with sc.exe..."
    Start-MySqlServiceBySc -ServiceName $mysqlService.Name
}
else {
    Write-Info "MySQL service is already running."
}

if (-not (Wait-MySqlReady -MySqlExe $mysqlExe)) {
    Write-Fail "MySQL service is running, but the database connection test failed."
    Write-Host "Please check host, port, username and password: ${DbUser}@${DbHost}:${DbPort}" -ForegroundColor Yellow
    Read-Host "Press Enter to exit"
    exit 1
}

Write-Info "Database connection test passed."

Write-Step "Step 4: Handle database initialization option"

if ($RebuildDatabase) {
    Write-Warn "The database '$DbName' will be recreated from the SQL script."
    Invoke-MySqlSqlFile -MySqlExe $mysqlExe -SqlFilePath $SqlFile
    Write-Info "Database import completed."
}
else {
    Write-Info "Database rebuild was skipped. Existing data will be preserved."
}

Write-Step "Step 5: Open backend terminal"

$backendCommand = @"
Set-Location '.\backend'
`$env:DB_HOST = '$DbHost'
`$env:DB_PORT = '$DbPort'
`$env:DB_NAME = '$DbName'
`$env:DB_USERNAME = '$DbUser'
`$env:DB_PASSWORD = '$DbPassword'
Write-Host 'Starting backend...' -ForegroundColor Cyan
mvn spring-boot:run
"@

Start-Process -FilePath "powershell.exe" -WorkingDirectory $ProjectRoot -ArgumentList @(
    "-NoExit",
    "-Command",
    $backendCommand
)

Write-Info "Backend terminal opened."

Write-Step "Step 6: Open frontend terminal"

$frontendCommand = @"
Set-Location '.\frontend'
if (-not (Test-Path 'node_modules')) {
    Write-Host 'Installing frontend dependencies for the first run...' -ForegroundColor Yellow
    npm install
}
Write-Host 'Starting frontend...' -ForegroundColor Cyan
npm run dev
"@

Start-Process -FilePath "powershell.exe" -WorkingDirectory $ProjectRoot -ArgumentList @(
    "-NoExit",
    "-Command",
    $frontendCommand
)

Write-Info "Frontend terminal opened."

Write-Step "Startup complete"
Write-Host "Frontend URL: http://localhost:5173" -ForegroundColor Green
Write-Host "Backend URL:  http://localhost:18080" -ForegroundColor Green
Write-Host ""
Write-Host "Wait for both new terminals to finish starting, then open the frontend URL in a browser." -ForegroundColor Yellow
Read-Host "Press Enter to close this launcher window"
