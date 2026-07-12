@echo off
setlocal

set "SCRIPT_DIR=%~dp0"
set "PS_SCRIPT=%SCRIPT_DIR%build_installer.ps1"

cd /d "%SCRIPT_DIR%"
powershell.exe -ExecutionPolicy Bypass -File "%PS_SCRIPT%" -SkipTests

if errorlevel 1 (
    echo.
    echo Installer build failed.
    pause
)

exit /b %errorlevel%
