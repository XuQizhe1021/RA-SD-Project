Set-StrictMode -Version Latest

$script:InstallRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$script:AppDir = Join-Path $script:InstallRoot "app"
$script:RuntimeDir = Join-Path $script:InstallRoot "runtime"
$script:ScriptDataDir = Join-Path $script:InstallRoot "data"
$script:LogDir = Join-Path $script:AppDir "logs"
$script:JavaRuntimeDir = Join-Path $script:RuntimeDir "jre"
$script:MySqlRoot = Join-Path $script:RuntimeDir "mysql"
$script:MySqlBinDir = Join-Path $script:MySqlRoot "bin"
$script:MySqlConfDir = Join-Path $script:MySqlRoot "conf"
$script:MySqlDataDir = Join-Path $script:MySqlRoot "data"
$script:MySqlDefaultsFile = Join-Path $script:MySqlConfDir "my.ini"
$script:DbInitSqlFile = Join-Path $script:AppDir "db\001_init.sql"
$script:AppConfigFile = Join-Path $script:AppDir "application-prod.yml"
$script:BackendJarFile = Join-Path $script:AppDir "hq-training-backend.jar"
$script:DbFlagFile = Join-Path $script:ScriptDataDir "db_initialized.flag"
$script:DbPidFile = Join-Path $script:ScriptDataDir "mysql.pid"
$script:BackendPidFile = Join-Path $script:ScriptDataDir "backend.pid"
$script:DbHost = "127.0.0.1"
$script:DbPort = 23306
$script:DbName = "hq_training"
$script:DbRootPassword = "HQRoot@2026"
$script:DbAppUser = "hq_app"
$script:DbAppPassword = "HQApp@2026"
$script:ServerPort = 18080
$script:ApplicationUrl = "http://127.0.0.1:$($script:ServerPort)"

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

function Ensure-Directory {
    param([string]$Path)

    if (-not (Test-Path $Path)) {
        New-Item -ItemType Directory -Path $Path -Force | Out-Null
    }
}

function Ensure-BaseDirectories {
    foreach ($path in @($script:AppDir, $script:RuntimeDir, $script:ScriptDataDir, $script:LogDir, $script:MySqlDataDir)) {
        Ensure-Directory -Path $path
    }
}

function Get-ShortPath {
    param([string]$Path)

    if (-not (Test-Path $Path)) {
        return $Path
    }

    $resolvedPath = (Resolve-Path $Path).Path
    $shortPath = cmd.exe /c "for %I in (""$resolvedPath"") do @echo %~sI"
    if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($shortPath)) {
        return $resolvedPath
    }

    return $shortPath.Trim()
}

function Get-JavaExecutable {
    $javaExe = Join-Path $script:JavaRuntimeDir "bin\java.exe"
    if (-not (Test-Path $javaExe)) {
        throw "Embedded java.exe was not found."
    }

    return $javaExe
}

function Get-MySqlExecutable {
    param([string]$ExecutableName)

    $path = Join-Path $script:MySqlBinDir $ExecutableName
    if (-not (Test-Path $path)) {
        throw "Embedded MySQL executable was not found: $ExecutableName"
    }

    return $path
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

function Wait-TcpPort {
    param(
        [string]$HostName,
        [int]$Port,
        [int]$MaxAttempts = 30,
        [int]$DelaySeconds = 2
    )

    for ($i = 1; $i -le $MaxAttempts; $i++) {
        if (Test-TcpPort -HostName $HostName -Port $Port) {
            return $true
        }

        Start-Sleep -Seconds $DelaySeconds
    }

    return $false
}

function Wait-PortClosed {
    param(
        [string]$HostName,
        [int]$Port,
        [int]$MaxAttempts = 15,
        [int]$DelaySeconds = 2
    )

    for ($i = 1; $i -le $MaxAttempts; $i++) {
        if (-not (Test-TcpPort -HostName $HostName -Port $Port)) {
            return $true
        }

        Start-Sleep -Seconds $DelaySeconds
    }

    return $false
}

function Wait-HttpReady {
    param(
        [string]$Url,
        [int]$MaxAttempts = 30,
        [int]$DelaySeconds = 2
    )

    for ($i = 1; $i -le $MaxAttempts; $i++) {
        try {
            $response = Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec 2
            if ($response.StatusCode -ge 200 -and $response.StatusCode -lt 500) {
                return $true
            }
        }
        catch {
            Start-Sleep -Seconds $DelaySeconds
        }
    }

    return $false
}

function Get-PidFromFile {
    param([string]$PidFile)

    if (-not (Test-Path $PidFile)) {
        return $null
    }

    $content = (Get-Content -Path $PidFile -ErrorAction SilentlyContinue | Select-Object -First 1)
    if ([string]::IsNullOrWhiteSpace($content)) {
        return $null
    }

    return [int]$content.Trim()
}

function Stop-ManagedProcess {
    param([string]$PidFile)

    $pidValue = Get-PidFromFile -PidFile $PidFile
    if (-not $pidValue) {
        return
    }

    $process = Get-Process -Id $pidValue -ErrorAction SilentlyContinue
    if ($process) {
        Stop-Process -Id $pidValue -Force -ErrorAction SilentlyContinue
    }

    Remove-Item -Path $PidFile -Force -ErrorAction SilentlyContinue
}

function Start-LoggedProcess {
    param(
        [string]$FilePath,
        [string[]]$ArgumentList,
        [string]$WorkingDirectory,
        [string]$PidFile,
        [string]$StdOutFile,
        [string]$StdErrFile
    )

    Ensure-Directory -Path (Split-Path -Parent $StdOutFile)
    Ensure-Directory -Path (Split-Path -Parent $StdErrFile)
    Remove-Item -Path $PidFile -Force -ErrorAction SilentlyContinue

    $process = Start-Process -FilePath $FilePath `
        -ArgumentList $ArgumentList `
        -WorkingDirectory $WorkingDirectory `
        -RedirectStandardOutput $StdOutFile `
        -RedirectStandardError $StdErrFile `
        -PassThru `
        -WindowStyle Hidden

    Set-Content -Path $PidFile -Value $process.Id -Encoding ASCII
    return $process
}

function New-FileLocationArgument {
    param([string]$Path)

    return "file:/$($Path -replace '\\', '/')"
}

function Get-MySqlConnectionArguments {
    param(
        [string]$UserName,
        [string]$Password,
        [string]$HostName = $script:DbHost,
        [int]$Port = $script:DbPort,
        [string]$Database
    )

    $arguments = @(
        "--default-character-set=utf8mb4",
        "--protocol=tcp",
        "-h$HostName",
        "-P$Port",
        "-u$UserName",
        "--connect-timeout=5"
    )

    if (-not [string]::IsNullOrEmpty($Password)) {
        $arguments += "--password=$Password"
    }

    if (-not [string]::IsNullOrEmpty($Database)) {
        $arguments += $Database
    }

    return $arguments
}

function Invoke-MySqlCommand {
    param(
        [string]$Sql,
        [string]$UserName,
        [string]$Password,
        [string]$HostName = $script:DbHost,
        [int]$Port = $script:DbPort,
        [string]$Database
    )

    $mysqlExe = Get-ShortPath -Path (Get-MySqlExecutable -ExecutableName "mysql.exe")
    $arguments = Get-MySqlConnectionArguments -UserName $UserName -Password $Password -HostName $HostName -Port $Port -Database $Database
    $arguments += "--execute=$Sql"

    & $mysqlExe @arguments
    if ($LASTEXITCODE -ne 0) {
        throw "mysql command failed."
    }
}

function Invoke-MySqlSqlFile {
    param(
        [string]$SqlFilePath,
        [string]$UserName,
        [string]$Password,
        [string]$HostName = $script:DbHost,
        [int]$Port = $script:DbPort,
        [string]$Database
    )

    if (-not (Test-Path $SqlFilePath)) {
        throw "SQL file was not found: $SqlFilePath"
    }

    $mysqlExe = Get-ShortPath -Path (Get-MySqlExecutable -ExecutableName "mysql.exe")
    $sqlFilePath = Get-ShortPath -Path $SqlFilePath
    $baseArguments = Get-MySqlConnectionArguments -UserName $UserName -Password $Password -HostName $HostName -Port $Port -Database $Database
    $commandLine = '"' + $mysqlExe + '" ' + ($baseArguments -join " ") + ' < "' + $sqlFilePath + '"'

    cmd.exe /c $commandLine | Out-Null
    if ($LASTEXITCODE -ne 0) {
        throw "mysql import failed."
    }
}

function Invoke-MySqlAdminShutdown {
    param(
        [string]$UserName,
        [string]$Password,
        [string]$HostName = $script:DbHost,
        [int]$Port = $script:DbPort
    )

    $mysqlAdminExe = Get-ShortPath -Path (Get-MySqlExecutable -ExecutableName "mysqladmin.exe")
    $arguments = @(
        "--protocol=tcp",
        "-h$HostName",
        "-P$Port",
        "-u$UserName"
    )

    if (-not [string]::IsNullOrEmpty($Password)) {
        $arguments += "--password=$Password"
    }

    $arguments += "shutdown"

    & $mysqlAdminExe @arguments | Out-Null
    if ($LASTEXITCODE -ne 0) {
        throw "mysqladmin shutdown failed."
    }
}
