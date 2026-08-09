@echo off
REM ============================================================
REM Send Sample Script (Windows)
REM Uploads sample CSV to LocalStack S3 input bucket, waits for
REM Lambda processing, and displays the JSON output.
REM ============================================================

setlocal

set ENDPOINT=http://localhost:4566
set INPUT_BUCKET=csv-input-bucket
set OUTPUT_BUCKET=csv-output-bucket
set REGION=us-east-1
set AWS_CMD=aws --endpoint-url=%ENDPOINT% --region %REGION%

REM Disable trailing checksums (AWS CLI v2 feature not supported by LocalStack v2)
set AWS_REQUEST_CHECKSUM_CALCULATION=WHEN_REQUIRED
set AWS_RESPONSE_CHECKSUM_VALIDATION=WHEN_REQUIRED

echo ============================================
echo  Sending sample CSV to LocalStack
echo ============================================

echo.
echo --- Uploading test CSV ---
%AWS_CMD% s3 cp sample-data\orders.csv s3://%INPUT_BUCKET%/orders.csv
echo Uploaded: orders.csv

echo.
echo --- Waiting for Lambda to process (10 seconds) ---
timeout /t 10 /nobreak >nul

echo.
echo --- Checking output ---
%AWS_CMD% s3 cp s3://%OUTPUT_BUCKET%/orders.json -

echo.
echo ============================================
echo  Done!
echo ============================================
