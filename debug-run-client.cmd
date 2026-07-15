@echo off
setlocal EnableExtensions DisableDelayedExpansion

for %%I in ("%~dp0.") do set "PROJECT_ROOT=%%~fI"
set "CAPTURE_SCRIPT=%PROJECT_ROOT%\tools\capture-native-crash.ps1"

if not exist "%CAPTURE_SCRIPT%" (
    echo [ERROR] Capture script not found: "%CAPTURE_SCRIPT%"
    exit /b 2
)

if /I "%~1"=="--dry-run" goto dry_run
if /I "%~1"=="--elevated" goto elevated

fltmc.exe >nul 2>&1
if not errorlevel 1 goto run_capture

echo [INFO] Administrator privileges are required for WPR capture.
echo [INFO] Requesting elevation through User Account Control...
set "ACTINIUM_DEBUG_LAUNCHER=%~f0"
powershell.exe -NoProfile -ExecutionPolicy Bypass -Command "$commandLine = '""' + $env:ACTINIUM_DEBUG_LAUNCHER + '" --elevated"'; $process = Start-Process -FilePath $env:ComSpec -ArgumentList @('/d', '/c', $commandLine) -Verb RunAs -Wait -PassThru; exit $process.ExitCode"
set "EXIT_CODE=%ERRORLEVEL%"
if not "%EXIT_CODE%"=="0" echo [ERROR] Elevated debug session failed or elevation was cancelled. Exit code: %EXIT_CODE%
exit /b %EXIT_CODE%

:elevated
fltmc.exe >nul 2>&1
if errorlevel 1 (
    echo [ERROR] The elevated launcher does not have administrator privileges.
    echo [ERROR] Debug capture was not started.
    pause
    exit /b 3
)

:run_capture
echo [INFO] Project root: "%PROJECT_ROOT%"
echo [INFO] Starting crash capture with WPR and LWJGL debug enabled.
echo [INFO] Close Minecraft normally when the test is complete.
echo.
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%CAPTURE_SCRIPT%" -ProjectRoot "%PROJECT_ROOT%" -EnableWpr -EnableLwjglDebug
set "EXIT_CODE=%ERRORLEVEL%"
echo.
if "%EXIT_CODE%"=="0" (
    echo [INFO] Debug capture session completed successfully.
) else (
    echo [ERROR] Debug capture session failed. Exit code: %EXIT_CODE%
)
echo [INFO] Review the session output above and run\crash-captures for collected files.
pause
exit /b %EXIT_CODE%

:dry_run
echo [INFO] Dry run: validating the capture setup without elevation or client startup.
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%CAPTURE_SCRIPT%" -ProjectRoot "%PROJECT_ROOT%" -EnableWpr -EnableLwjglDebug -DryRun
exit /b %ERRORLEVEL%
