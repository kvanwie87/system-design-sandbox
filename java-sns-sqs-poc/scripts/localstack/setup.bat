@echo off
setlocal enabledelayedexpansion

REM =============================================================================
REM LocalStack Infrastructure Setup (Windows)
REM Creates: S3 bucket, SNS topic, 2 SQS queues, Lambda function, S3 notification
REM
REM Usage: setup.bat [--profile <profile>]
REM =============================================================================

set PROFILE_ARG=

if "%~1"=="--profile" (
    set PROFILE_ARG=--profile %~2
)

set REGION=us-east-1
set ACCOUNT_ID=000000000000
set ENDPOINT=http://localhost:4566
set BUCKET_NAME=csv-input-bucket
set TOPIC_NAME=csv-processed
set QUEUE_AUDIT=audit-queue
set QUEUE_NOTIFICATION=notification-queue
set FUNCTION_NAME=csv-processor
set JAR_PATH=%~dp0..\..\build\libs\csv-processor-lambda-all.jar
set AWSCMD=aws --endpoint-url=%ENDPOINT% --region %REGION% %PROFILE_ARG%

echo ============================================
echo  LocalStack Infrastructure Setup
echo ============================================

REM --- Wait for LocalStack to be healthy ---
echo.
echo [1/8] Waiting for LocalStack to be healthy...
set RETRY=0
:healthcheck
curl -sf %ENDPOINT%/_localstack/health >nul 2>&1
if %errorlevel%==0 goto healthy
set /a RETRY+=1
if %RETRY% geq 30 (
    echo ERROR: LocalStack did not become healthy after 30 attempts
    exit /b 1
)
echo   Waiting... (attempt %RETRY%/30)
timeout /t 2 /nobreak >nul
goto healthcheck
:healthy
echo   LocalStack is healthy!

REM --- Create S3 bucket ---
echo.
echo [2/8] Creating S3 bucket: %BUCKET_NAME%
%AWSCMD% s3 mb s3://%BUCKET_NAME% 2>nul
if %errorlevel% neq 0 echo   Bucket already exists

REM --- Create SNS topic ---
echo.
echo [3/8] Creating SNS topic: %TOPIC_NAME%
for /f "tokens=*" %%i in ('%AWSCMD% sns create-topic --name %TOPIC_NAME% --query TopicArn --output text') do set TOPIC_ARN=%%i
echo   Topic ARN: %TOPIC_ARN%

REM --- Create SQS queues ---
echo.
echo [4/8] Creating SQS queues: %QUEUE_AUDIT%, %QUEUE_NOTIFICATION%
for /f "tokens=*" %%i in ('%AWSCMD% sqs create-queue --queue-name %QUEUE_AUDIT% --query QueueUrl --output text') do set AUDIT_QUEUE_URL=%%i
for /f "tokens=*" %%i in ('%AWSCMD% sqs create-queue --queue-name %QUEUE_NOTIFICATION% --query QueueUrl --output text') do set NOTIFICATION_QUEUE_URL=%%i
echo   Audit queue URL: %AUDIT_QUEUE_URL%
echo   Notification queue URL: %NOTIFICATION_QUEUE_URL%

set AUDIT_QUEUE_ARN=arn:aws:sqs:%REGION%:%ACCOUNT_ID%:%QUEUE_AUDIT%
set NOTIFICATION_QUEUE_ARN=arn:aws:sqs:%REGION%:%ACCOUNT_ID%:%QUEUE_NOTIFICATION%

REM --- Subscribe queues to SNS topic ---
echo.
echo [5/8] Subscribing queues to SNS topic
%AWSCMD% sns subscribe --topic-arn %TOPIC_ARN% --protocol sqs --notification-endpoint %AUDIT_QUEUE_ARN% --output text >nul
echo   Subscribed: %QUEUE_AUDIT%
%AWSCMD% sns subscribe --topic-arn %TOPIC_ARN% --protocol sqs --notification-endpoint %NOTIFICATION_QUEUE_ARN% --output text >nul
echo   Subscribed: %QUEUE_NOTIFICATION%

REM --- Create IAM role for Lambda ---
echo.
echo [6/8] Creating IAM role for Lambda
set ROLE_NAME=lambda-csv-processor-role

REM Write assume role policy to temp file via PowerShell
powershell -NoProfile -Command "[IO.File]::WriteAllText(\"$env:TEMP\assume_role_policy.json\", (ConvertTo-Json -Compress -Depth 5 @{Version='2012-10-17';Statement=@(@{Effect='Allow';Principal=@{Service='lambda.amazonaws.com'};Action='sts:AssumeRole'})}))"

%AWSCMD% iam create-role --role-name %ROLE_NAME% --assume-role-policy-document file://%TEMP%\assume_role_policy.json --output text >nul 2>&1
if %errorlevel% neq 0 echo   Role already exists

set ROLE_ARN=arn:aws:iam::%ACCOUNT_ID%:role/%ROLE_NAME%
echo   Role ARN: %ROLE_ARN%

REM --- Deploy Lambda function ---
echo.
echo [7/8] Deploying Lambda function: %FUNCTION_NAME%
if not exist "%JAR_PATH%" (
    echo ERROR: Shadow JAR not found at %JAR_PATH%
    echo Run 'gradlew shadowJar' first.
    exit /b 1
)

REM Delete existing function if present (for idempotency)
%AWSCMD% lambda delete-function --function-name %FUNCTION_NAME% 2>nul

%AWSCMD% lambda create-function --function-name %FUNCTION_NAME% --runtime java17 --handler "com.example.processor.CsvProcessorHandler" --role %ROLE_ARN% --zip-file "fileb://%JAR_PATH%" --timeout 60 --memory-size 512 --environment "Variables={AWS_ENDPOINT_URL=http://host.docker.internal:4566,SNS_TOPIC_ARN=%TOPIC_ARN%}" --output text >nul

echo   Lambda deployed successfully
echo   Waiting for Lambda to become available...
timeout /t 3 /nobreak >nul

REM --- Configure S3 bucket notification ---
echo.
echo [8/8] Configuring S3 bucket notification to trigger Lambda
set LAMBDA_ARN=arn:aws:lambda:%REGION%:%ACCOUNT_ID%:function:%FUNCTION_NAME%

powershell -NoProfile -Command "[IO.File]::WriteAllText('%TEMP%\s3_notification.json', (ConvertTo-Json -Compress -Depth 5 @{LambdaFunctionConfigurations=@(@{LambdaFunctionArn='%LAMBDA_ARN%';Events=@('s3:ObjectCreated:*')})}))"

%AWSCMD% s3api put-bucket-notification-configuration --bucket %BUCKET_NAME% --notification-configuration file://%TEMP%\s3_notification.json

echo   S3 notification configured

REM --- Done ---
echo.
echo ============================================
echo  Setup Complete!
echo ============================================
echo.
echo Resources created:
echo   S3 Bucket:    s3://%BUCKET_NAME%
echo   SNS Topic:    %TOPIC_ARN%
echo   SQS Queues:   %AUDIT_QUEUE_URL%
echo                 %NOTIFICATION_QUEUE_URL%
echo   Lambda:       %FUNCTION_NAME%
echo.
echo Test with:
echo   scripts\localstack\test-e2e.bat

REM Cleanup temp files
del "%TEMP%\assume_role_policy.json" 2>nul

endlocal
