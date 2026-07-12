@echo off
setlocal

set "SCRIPT_DIR=%~dp0"
set "PS_SCRIPT=%SCRIPT_DIR%launch_app.ps1"
set "PS_ARGS="

cd /d "%SCRIPT_DIR%"

echo.
echo ======================================================
echo HQ Training Management System Launcher
echo ======================================================
echo Rebuild database before startup?
echo Y = Reimport seed SQL and overwrite existing data.
echo N = Keep current database data and start normally.
echo.
choice /C YN /N /M "Select [Y/N, recommended N]: "

if errorlevel 2 (
    set "PS_ARGS="
) else (
    set "PS_ARGS=-RebuildDatabase"
)

powershell.exe -ExecutionPolicy Bypass -File "%PS_SCRIPT%" %PS_ARGS%

if errorlevel 1 (
    echo.
    echo Launch failed.
    pause
)

exit /b %errorlevel%
