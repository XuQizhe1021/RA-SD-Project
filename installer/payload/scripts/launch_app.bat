@echo off
setlocal

set "SCRIPT_DIR=%~dp0"
powershell.exe -ExecutionPolicy Bypass -File "%SCRIPT_DIR%launch_app.ps1"

if errorlevel 1 (
    echo.
    echo Launch failed.
    pause
)

exit /b %errorlevel%
