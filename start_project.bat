@echo off
setlocal

REM This batch file only opens PowerShell.
REM The PowerShell script handles elevation by itself.

set "SCRIPT_DIR=%~dp0"
set "PS_SCRIPT=%SCRIPT_DIR%start_project.ps1"

cd /d "%SCRIPT_DIR%"
echo Opening PowerShell launcher...
powershell.exe -NoExit -ExecutionPolicy Bypass -File "%PS_SCRIPT%"

if errorlevel 1 (
    echo.
    echo PowerShell launcher exited with an error.
    pause
)

exit /b %errorlevel%
