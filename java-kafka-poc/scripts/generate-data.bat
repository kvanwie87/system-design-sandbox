@echo off
REM Sends randomized sensor readings to the sensor-ingest service (Windows CMD).
REM This is a thin wrapper that delegates to the PowerShell version.
REM
REM Usage:
REM   scripts\generate-data.bat
REM   scripts\generate-data.bat -Count 50 -IntervalMs 500
REM   scripts\generate-data.bat -Sensors 10 -IntervalMs 200

setlocal
powershell -ExecutionPolicy Bypass -File "%~dp0generate-data.ps1" %*
endlocal
