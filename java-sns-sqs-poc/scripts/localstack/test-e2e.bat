@echo off
setlocal enabledelayedexpansion

REM =============================================================================
REM End-to-End Test Script for LocalStack (Windows)
REM Uploads a CSV to S3, waits for processing, and verifies messages in SQS queues
REM
REM Usage: test-e2e.bat [--profile <profile>]
REM =============================================================================

set PROFILE_ARG=

if "%~1"=="--profile" (
    set PROFILE_ARG=--profile %~2
)

set REGION=us-east-1
set ENDPOINT=http://localhost:4566
set BUCKET_NAME=csv-input-bucket
set QUEUE_AUDIT=audit-queue
set QUEUE_NOTIFICATION=notification-queue
set SAMPLE_FILE=%~dp0..\..\samples\test.csv
set WAIT_SECONDS=10
set AWSCMD=aws --endpoint-url=%ENDPOINT% --region %REGION% %PROFILE_ARG%

echo ============================================
echo  End-to-End Test (LocalStack)
echo ============================================

REM --- Verify LocalStack is running ---
echo.
echo [1/5] Checking LocalStack is healthy...
curl -sf %ENDPOINT%/_localstack/health >nul 2>&1
if %errorlevel% neq 0 (
    echo ERROR: LocalStack is not running. Start it with: docker-compose up -d
    exit /b 1
)
echo   OK

REM --- Get queue URLs ---
for /f "tokens=*" %%i in ('%AWSCMD% sqs get-queue-url --queue-name %QUEUE_AUDIT% --query QueueUrl --output text 2^>nul') do set AUDIT_QUEUE_URL=%%i
for /f "tokens=*" %%i in ('%AWSCMD% sqs get-queue-url --queue-name %QUEUE_NOTIFICATION% --query QueueUrl --output text 2^>nul') do set NOTIFICATION_QUEUE_URL=%%i

if "%AUDIT_QUEUE_URL%"=="" (
    echo ERROR: Queues not found. Run 'scripts\localstack\setup.bat' first.
    exit /b 1
)
if "%NOTIFICATION_QUEUE_URL%"=="" (
    echo ERROR: Queues not found. Run 'scripts\localstack\setup.bat' first.
    exit /b 1
)

REM --- Purge queues (clean state) ---
echo.
echo [2/5] Purging queues for clean test...
%AWSCMD% sqs purge-queue --queue-url %AUDIT_QUEUE_URL% 2>nul
%AWSCMD% sqs purge-queue --queue-url %NOTIFICATION_QUEUE_URL% 2>nul
echo   Queues purged

REM --- Upload CSV to S3 ---
echo.
echo [3/5] Uploading sample CSV to S3...
if not exist "%SAMPLE_FILE%" (
    echo ERROR: Sample file not found at %SAMPLE_FILE%
    exit /b 1
)
%AWSCMD% s3 cp "%SAMPLE_FILE%" s3://%BUCKET_NAME%/test.csv
echo   Uploaded: s3://%BUCKET_NAME%/test.csv

REM --- Wait for async processing ---
echo.
echo [4/5] Waiting %WAIT_SECONDS%s for Lambda processing...
timeout /t %WAIT_SECONDS% /nobreak >nul

REM --- Check SQS queues for messages ---
echo.
echo [5/5] Checking SQS queues for messages...

echo.
echo --- Audit Queue Message ---
%AWSCMD% sqs receive-message --queue-url %AUDIT_QUEUE_URL% --max-number-of-messages 1 --wait-time-seconds 5 --output json > "%TEMP%\audit_msg.json"
type "%TEMP%\audit_msg.json"

echo.
echo --- Notification Queue Message ---
%AWSCMD% sqs receive-message --queue-url %NOTIFICATION_QUEUE_URL% --max-number-of-messages 1 --wait-time-seconds 5 --output json > "%TEMP%\notification_msg.json"
type "%TEMP%\notification_msg.json"

REM --- Validate results ---
echo.
echo ============================================
echo  Validation
echo ============================================

set PASS=true

findstr /c:"Messages" "%TEMP%\audit_msg.json" >nul 2>&1
if %errorlevel%==0 (
    echo   [PASS] Audit queue received a message
) else (
    echo   [FAIL] Audit queue is empty
    set PASS=false
)

findstr /c:"Messages" "%TEMP%\notification_msg.json" >nul 2>&1
if %errorlevel%==0 (
    echo   [PASS] Notification queue received a message
) else (
    echo   [FAIL] Notification queue is empty
    set PASS=false
)

findstr /c:"rowCount" "%TEMP%\audit_msg.json" >nul 2>&1
if %errorlevel%==0 (
    echo   [PASS] Message contains rowCount field
) else (
    echo   [WARN] Could not verify rowCount in message
)

echo.
if "%PASS%"=="true" (
    echo   *** ALL TESTS PASSED ***
    del "%TEMP%\audit_msg.json" 2>nul
    del "%TEMP%\notification_msg.json" 2>nul
    exit /b 0
) else (
    echo   *** TESTS FAILED ***
    del "%TEMP%\audit_msg.json" 2>nul
    del "%TEMP%\notification_msg.json" 2>nul
    exit /b 1
)

endlocal
