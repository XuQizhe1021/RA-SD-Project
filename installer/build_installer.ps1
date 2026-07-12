[CmdletBinding()]
param(
    [switch]$SkipAppBuild,
    [switch]$SkipTests,
    [switch]$PrepareOnly,
    [string]$MySqlRuntimeDir,
    [string]$JavaHome,
    [string]$IsccPath,
    [ValidateSet("Auto", "JLink", "CopyJavaHome")]
    [string]$JavaRuntimeMode = "Auto"
)

$ErrorActionPreference = "Stop"

$ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$InstallerRoot = $PSScriptRoot
$PayloadRoot = Join-Path $InstallerRoot "payload"
$StagingRoot = Join-Path $InstallerRoot "staging\HQTraining"
$OutputRoot = Join-Path $InstallerRoot "output"
$BackendRoot = Join-Path $ProjectRoot "backend"
$SqlSourceFile = Join-Path $BackendRoot "src\main\resources\db\mysql\001_init.sql"
$PayloadScriptsRoot = Join-Path $PayloadRoot "scripts"
$DbAppUser = "hq_app"
$DbAppPassword = "HQApp@2026"
$DbPort = 23306

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

function Get-LatestJarFile {
    $targetRoot = Join-Path $BackendRoot "target"
    return Get-ChildItem -Path $targetRoot -Filter "*.jar" |
        Where-Object { $_.Name -notlike "*.original" } |
        Sort-Object LastWriteTime -Descending |
        Select-Object -First 1
}

function Resolve-JavaHome {
    param([string]$ExplicitJavaHome)

    if (-not [string]::IsNullOrWhiteSpace($ExplicitJavaHome)) {
        return (Resolve-Path $ExplicitJavaHome).Path
    }

    if ($env:JAVA_HOME) {
        return (Resolve-Path $env:JAVA_HOME).Path
    }

    $javaCommand = Get-Command java -ErrorAction SilentlyContinue
    if (-not $javaCommand) {
        throw "java.exe was not found."
    }

    return Split-Path -Parent (Split-Path -Parent $javaCommand.Source)
}

function Resolve-MySqlRuntimeDir {
    param([string]$ExplicitMySqlRuntimeDir)

    if (-not [string]::IsNullOrWhiteSpace($ExplicitMySqlRuntimeDir)) {
        return (Resolve-Path $ExplicitMySqlRuntimeDir).Path
    }

    $mysqldCommand = Get-Command mysqld.exe -ErrorAction SilentlyContinue
    if ($mysqldCommand) {
        return Split-Path -Parent (Split-Path -Parent $mysqldCommand.Source)
    }

    $service = Get-CimInstance Win32_Service | Where-Object {
        $_.Name -match "mysql" -or $_.DisplayName -match "mysql"
    } | Select-Object -First 1

    if ($service -and -not [string]::IsNullOrWhiteSpace($service.PathName)) {
        $pathName = $service.PathName.Trim()
        if ($pathName.StartsWith('"')) {
            $serviceExe = $pathName.Split('"')[1]
        }
        else {
            $serviceExe = $pathName.Split(" ")[0]
        }

        if (Test-Path $serviceExe) {
            return Split-Path -Parent (Split-Path -Parent $serviceExe)
        }
    }

    throw "MySQL runtime directory was not found. Use -MySqlRuntimeDir to specify it explicitly."
}

function Resolve-IsccPath {
    param([string]$ExplicitIsccPath)

    if (-not [string]::IsNullOrWhiteSpace($ExplicitIsccPath)) {
        return (Resolve-Path $ExplicitIsccPath).Path
    }

    foreach ($path in @(
        "C:\Program Files (x86)\Inno Setup 6\ISCC.exe",
        "C:\Program Files\Inno Setup 6\ISCC.exe",
        (Join-Path $env:LOCALAPPDATA "Programs\Inno Setup 6\ISCC.exe")
    )) {
        if (Test-Path $path) {
            return $path
        }
    }

    return $null
}

function Copy-DirectoryContents {
    param(
        [string]$SourcePath,
        [string]$DestinationPath
    )

    Ensure-Directory -Path $DestinationPath
    Copy-Item -Path (Join-Path $SourcePath "*") -Destination $DestinationPath -Recurse -Force
}

function New-GeneratedAppConfig {
    param([string]$Path)

    $content = @"
server:
  port: 18080

spring:
  application:
    name: hq-training-backend
  datasource:
    url: jdbc:mysql://127.0.0.1:$DbPort/hq_training?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false
    username: $DbAppUser
    password: $DbAppPassword
    driver-class-name: com.mysql.cj.jdbc.Driver

logging:
  file:
    path: ./app/logs

app:
  jwt:
    secret: hq-training-system-secret-key-change-before-production
    expiration-hours: 8
"@

    Set-Content -Path $Path -Value $content -Encoding UTF8
}

function New-GeneratedMySqlConfig {
    param([string]$Path)

    $content = @"
[mysqld]
default-time-zone=+08:00
character-set-server=utf8mb4
collation-server=utf8mb4_general_ci
default_authentication_plugin=mysql_native_password
max_connections=200

[client]
default-character-set=utf8mb4
"@

    Set-Content -Path $Path -Value $content -Encoding ASCII
}

function Prepare-JavaRuntime {
    param(
        [string]$ResolvedJavaHome,
        [string]$DestinationPath,
        [string]$RuntimeMode
    )

    $resolvedMode = $RuntimeMode
    $jlinkExe = Join-Path $ResolvedJavaHome "bin\jlink.exe"
    $jmodsDir = Join-Path $ResolvedJavaHome "jmods"

    if ($RuntimeMode -eq "Auto") {
        if ((Test-Path $jlinkExe) -and (Test-Path $jmodsDir)) {
            $resolvedMode = "JLink"
        }
        else {
            $resolvedMode = "CopyJavaHome"
        }
    }

    if ($resolvedMode -eq "JLink") {
        $modules = @(
            "java.base",
            "java.desktop",
            "java.instrument",
            "java.logging",
            "java.management",
            "java.naming",
            "java.net.http",
            "java.security.jgss",
            "java.sql",
            "java.transaction.xa",
            "java.xml",
            "jdk.crypto.ec",
            "jdk.unsupported"
        ) -join ","

        & $jlinkExe `
            "--module-path" $jmodsDir `
            "--add-modules" $modules `
            "--strip-debug" `
            "--no-header-files" `
            "--no-man-pages" `
            "--output" $DestinationPath

        if ($LASTEXITCODE -ne 0) {
            throw "jlink runtime generation failed."
        }

        Write-Info "Embedded Java runtime was created with jlink."
        return
    }

    Copy-Item -Path $ResolvedJavaHome -Destination $DestinationPath -Recurse -Force
    Write-Info "Embedded Java runtime was copied from the current Java home."
}

Write-Step "Step 1: Build delivery package"
if (-not $SkipAppBuild) {
    $buildScript = Join-Path $ProjectRoot "build_delivery.ps1"
    & $buildScript -SkipTests:$SkipTests
}

$jarFile = Get-LatestJarFile
if (-not $jarFile) {
    throw "Packaged backend jar was not found. Run build_delivery.ps1 first."
}

Write-Step "Step 2: Resolve runtime sources"
$resolvedJavaHome = Resolve-JavaHome -ExplicitJavaHome $JavaHome
$resolvedMySqlRuntimeDir = Resolve-MySqlRuntimeDir -ExplicitMySqlRuntimeDir $MySqlRuntimeDir
$resolvedIsccPath = Resolve-IsccPath -ExplicitIsccPath $IsccPath

Write-Info "Java home: $resolvedJavaHome"
Write-Info "MySQL runtime: $resolvedMySqlRuntimeDir"
if ($resolvedIsccPath) {
    Write-Info "Inno Setup compiler: $resolvedIsccPath"
}
else {
    Write-Warn "ISCC.exe was not found. Staging files will still be prepared."
}

Write-Step "Step 3: Prepare staging directory"
if (Test-Path $StagingRoot) {
    Remove-Item -Path $StagingRoot -Recurse -Force
}

Ensure-Directory -Path $StagingRoot
Ensure-Directory -Path (Join-Path $StagingRoot "app")
Ensure-Directory -Path (Join-Path $StagingRoot "app\db")
Ensure-Directory -Path (Join-Path $StagingRoot "app\logs")
Ensure-Directory -Path (Join-Path $StagingRoot "runtime")
Ensure-Directory -Path (Join-Path $StagingRoot "runtime\mysql")
Ensure-Directory -Path (Join-Path $StagingRoot "runtime\mysql\conf")
Ensure-Directory -Path (Join-Path $StagingRoot "runtime\mysql\data")
Ensure-Directory -Path (Join-Path $StagingRoot "scripts")
Ensure-Directory -Path (Join-Path $StagingRoot "data")
Ensure-Directory -Path $OutputRoot

Copy-Item -Path $jarFile.FullName -Destination (Join-Path $StagingRoot "app\hq-training-backend.jar") -Force
Copy-Item -Path $SqlSourceFile -Destination (Join-Path $StagingRoot "app\db\001_init.sql") -Force
Copy-DirectoryContents -SourcePath $PayloadScriptsRoot -DestinationPath (Join-Path $StagingRoot "scripts")
New-GeneratedAppConfig -Path (Join-Path $StagingRoot "app\application-prod.yml")
New-GeneratedMySqlConfig -Path (Join-Path $StagingRoot "runtime\mysql\conf\my.ini")

Write-Step "Step 4: Prepare embedded Java runtime"
Prepare-JavaRuntime `
    -ResolvedJavaHome $resolvedJavaHome `
    -DestinationPath (Join-Path $StagingRoot "runtime\jre") `
    -RuntimeMode $JavaRuntimeMode

Write-Step "Step 5: Prepare private MySQL runtime"
foreach ($entry in @("bin", "lib", "share", "LICENSE", "README")) {
    $source = Join-Path $resolvedMySqlRuntimeDir $entry
    $destination = Join-Path $StagingRoot "runtime\mysql\$entry"
    if (Test-Path $source) {
        if ((Get-Item $source).PSIsContainer) {
            Copy-Item -Path $source -Destination $destination -Recurse -Force
        }
        else {
            Ensure-Directory -Path (Split-Path -Parent $destination)
            Copy-Item -Path $source -Destination $destination -Force
        }
    }
}

Write-Info "Installer staging directory:"
Write-Host $StagingRoot -ForegroundColor Green

if ($PrepareOnly) {
    Write-Warn "PrepareOnly was specified. Installer compilation was skipped."
    exit 0
}

if (-not $resolvedIsccPath) {
    Write-Warn "Install Inno Setup 6, then rerun this script to produce the final installer exe."
    exit 0
}

Write-Step "Step 6: Build installer exe"
& $resolvedIsccPath `
    "/DAppSource=$StagingRoot" `
    "/DOutputDir=$OutputRoot" `
    (Join-Path $InstallerRoot "app.iss")

if ($LASTEXITCODE -ne 0) {
    throw "Inno Setup compilation failed."
}

Write-Info "Installer output directory:"
Write-Host $OutputRoot -ForegroundColor Green
