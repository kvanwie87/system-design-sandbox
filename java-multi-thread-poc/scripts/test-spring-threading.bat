@echo off
REM ============================================================
REM Test script for :spring-threading module (port 8080)
REM Sends sample payloads to all endpoints and displays responses.
REM
REM Prerequisites: the app must be running on port 8080
REM   gradlew.bat :spring-threading:bootRun
REM ============================================================

set BASE_URL=http://localhost:8080

echo ========================================
echo  Testing :spring-threading (port 8080)
echo ========================================

REM --- Fire-and-forget ---
echo.
echo --- 1. Fire-and-Forget: POST /notifications/send ---
echo Sending notification...
curl -s -w "\nHTTP Status: %%{http_code}\n" -X POST "%BASE_URL%/notifications/send" -H "Content-Type: application/json" -d "{\"message\": \"Hello from test script!\"}"
echo.
echo (Check app logs — notification completes ~2s later on an async thread)

REM --- Async with Polling ---
echo.
echo.
echo --- 2. Async with Polling: POST /reports/generate ---
echo Starting report generation...

REM Save response to temp file to extract taskId
curl -s -X POST "%BASE_URL%/reports/generate" > %TEMP%\report_response.txt
set /p RESPONSE=<%TEMP%\report_response.txt
echo Response: %RESPONSE%

REM Extract taskId using PowerShell
for /f "usebackq delims=" %%i in (`powershell -Command "(Get-Content '%TEMP%\report_response.txt' | ConvertFrom-Json).taskId"`) do set TASK_ID=%%i

if "%TASK_ID%"=="" (
    echo ERROR: Could not extract taskId from response
    exit /b 1
)

echo.
echo --- 2a. Poll: GET /reports/%TASK_ID% (immediately — should be PENDING) ---
curl -s -w "\nHTTP Status: %%{http_code}\n" "%BASE_URL%/reports/%TASK_ID%"

echo.
echo Waiting 6 seconds for report to complete...
timeout /t 6 /nobreak > nul

echo.
echo --- 2b. Poll: GET /reports/%TASK_ID% (after 6s — should be COMPLETE) ---
curl -s -w "\nHTTP Status: %%{http_code}\n" "%BASE_URL%/reports/%TASK_ID%"

REM --- Unknown task ID ---
echo.
echo.
echo --- 3. Edge case: GET /reports/unknown-id (should be 404) ---
curl -s -w "\nHTTP Status: %%{http_code}\n" "%BASE_URL%/reports/does-not-exist"

echo.
echo.
echo ========================================
echo  Done! Check app logs for async output.
echo ========================================

REM Cleanup
del %TEMP%\report_response.txt 2>nul
