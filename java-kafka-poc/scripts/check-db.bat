@echo off
REM Validates that sensor readings are being persisted to PostgreSQL (Windows CMD).
REM This is a thin wrapper that delegates to the PowerShell version.
REM
REM Usage:
REM   scripts\check-db.bat              Summary + latest 10
REM   scripts\check-db.bat count        Just the total count
REM   scripts\check-db.bat watch        Poll every 2 seconds
REM   scripts\check-db.bat sensors      Per-sensor breakdown
REM   scripts\check-db.bat alerts       Show alert-level readings

setlocal
powershell -ExecutionPolicy Bypass -File "%~dp0check-db.ps1" %*
endlocal
