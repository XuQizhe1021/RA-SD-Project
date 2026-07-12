# Build the delivery-style package assets:
# 1. build frontend
# 2. copy static files into backend resources
# 3. package backend jar

[CmdletBinding()]
param(
    [switch]$SkipTests
)

$ErrorActionPreference = "Stop"

$ProjectRoot = $PSScriptRoot
$FrontendRoot = Join-Path $ProjectRoot "frontend"
$BackendRoot = Join-Path $ProjectRoot "backend"
$FrontendDist = Join-Path $FrontendRoot "dist"
$BackendStatic = Join-Path $BackendRoot "src\main\resources\static"

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

function Assert-CommandExists {
    param([string]$CommandName)

    if (-not (Get-Command $CommandName -ErrorAction SilentlyContinue)) {
        throw "Command not found: $CommandName"
    }
}

Write-Step "Step 1: Check required tools"
Assert-CommandExists -CommandName "npm"
Assert-CommandExists -CommandName "mvn"
Write-Info "npm and Maven are available."

Write-Step "Step 2: Build frontend"
if (-not (Test-Path (Join-Path $FrontendRoot "node_modules"))) {
    Push-Location $FrontendRoot
    try {
        Write-Info "Installing frontend dependencies..."
        & npm install
        if ($LASTEXITCODE -ne 0) {
            throw "Frontend dependency installation failed."
        }
    }
    finally {
        Pop-Location
    }
}

Push-Location $FrontendRoot
try {
    & npm run build
    if ($LASTEXITCODE -ne 0) {
        throw "Frontend build failed."
    }
}
finally {
    Pop-Location
}

if (-not (Test-Path $FrontendDist)) {
    throw "Frontend build output not found: $FrontendDist"
}

Write-Step "Step 3: Copy static assets into backend"
if (-not (Test-Path $BackendStatic)) {
    New-Item -ItemType Directory -Path $BackendStatic | Out-Null
}
else {
    Get-ChildItem -Path $BackendStatic -Force | Remove-Item -Recurse -Force
}

Copy-Item -Path (Join-Path $FrontendDist "*") -Destination $BackendStatic -Recurse -Force
Write-Info "Frontend static assets were copied into backend resources."

Write-Step "Step 4: Package backend"
$mavenArgs = @("clean", "package")
if ($SkipTests) {
    $mavenArgs += "-DskipTests"
}

Push-Location $BackendRoot
try {
    & mvn @mavenArgs
    if ($LASTEXITCODE -ne 0) {
        throw "Backend package build failed."
    }
}
finally {
    Pop-Location
}

$jarFile = Get-ChildItem -Path (Join-Path $BackendRoot "target") -Filter "*.jar" |
    Where-Object { $_.Name -notlike "*.original" } |
    Sort-Object LastWriteTime -Descending |
    Select-Object -First 1

if (-not $jarFile) {
    throw "Backend package output was not found."
}

Write-Step "Build complete"
Write-Host "Packaged backend jar:" -ForegroundColor Green
Write-Host $jarFile.FullName -ForegroundColor Green
Write-Host ""
Write-Host "Run launch_app.bat or launch_app.ps1 to start the system." -ForegroundColor Yellow
