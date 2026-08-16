@echo off
setlocal enabledelayedexpansion

REM =============================================================================
REM AWS Infrastructure Teardown (Windows)
REM Removes all resources created by setup.bat
REM
REM Usage: teardown.bat [--profile <profile>]
REM   Environment: AWS_REGION (default: us-east-1)
REM =============================================================================

set PROFILE_ARG=

if "%~1"=="--profile" (
    set PROFILE_ARG=--profile %~2
)

if "%AWS_REGION%"=="" (set REGION=us-east-1) else (set REGION=%AWS_REGION%)

set TOPIC_NAME=csv-processed
set QUEUE_AUDIT=audit-queue
set QUEUE_NOTIFICATION=notification-queue
set FUNCTION_NAME=csv-processor
set ROLE_NAME=lambda-csv-processor-role
set AWSCMD=aws --region %REGION% %PROFILE_ARG%

for /f "tokens=*" %%i in ('%AWSCMD% sts get-caller-identity --query Account --output text') do set ACCOUNT_ID=%%i
set BUCKET_NAME=csv-input-bucket-%ACCOUNT_ID%
set TOPIC_ARN=arn:aws:sns:%REGION%:%ACCOUNT_ID%:%TOPIC_NAME%

echo ============================================
echo  AWS Infrastructure Teardown
echo ============================================
echo  Region:  %REGION%
echo  Account: %ACCOUNT_ID%
echo ============================================
echo.
echo WARNING: This will delete ALL resources created by setup.bat.
echo Press Ctrl+C to cancel, or wait 5 seconds to proceed...
timeout /t 5 /nobreak >nul

REM --- Remove S3 bucket notification ---
echo.
echo [1/7] Removing S3 bucket notification...
%AWSCMD% s3api put-bucket-notification-configuration --bucket %BUCKET_NAME% --notification-configuration "{}" 2>nul
if %errorlevel% neq 0 echo   Skipped (bucket may not exist)

REM --- Delete Lambda function ---
echo.
echo [2/7] Deleting Lambda function: %FUNCTION_NAME%
%AWSCMD% lambda delete-function --function-name %FUNCTION_NAME% 2>nul
if %errorlevel% neq 0 echo   Already deleted or not found

REM --- Remove SNS subscriptions ---
echo.
echo [3/7] Removing SNS subscriptions...
for /f "tokens=*" %%i in ('%AWSCMD% sns list-subscriptions-by-topic --topic-arn %TOPIC_ARN% --query "Subscriptions[].SubscriptionArn" --output text 2^>nul') do (
    if not "%%i"=="PendingConfirmation" (
        %AWSCMD% sns unsubscribe --subscription-arn %%i 2>nul
        echo   Unsubscribed: %%i
    )
)

REM --- Delete SNS topic ---
echo.
echo [4/7] Deleting SNS topic: %TOPIC_NAME%
%AWSCMD% sns delete-topic --topic-arn %TOPIC_ARN% 2>nul
if %errorlevel% neq 0 echo   Already deleted or not found

REM --- Delete SQS queues ---
echo.
echo [5/7] Deleting SQS queues...
for /f "tokens=*" %%i in ('%AWSCMD% sqs get-queue-url --queue-name %QUEUE_AUDIT% --query QueueUrl --output text 2^>nul') do (
    %AWSCMD% sqs delete-queue --queue-url %%i
    echo   Deleted: %QUEUE_AUDIT%
)
for /f "tokens=*" %%i in ('%AWSCMD% sqs get-queue-url --queue-name %QUEUE_NOTIFICATION% --query QueueUrl --output text 2^>nul') do (
    %AWSCMD% sqs delete-queue --queue-url %%i
    echo   Deleted: %QUEUE_NOTIFICATION%
)

REM --- Delete IAM role ---
echo.
echo [6/7] Deleting IAM role: %ROLE_NAME%
%AWSCMD% iam delete-role-policy --role-name %ROLE_NAME% --policy-name csv-processor-policy 2>nul
if %errorlevel% neq 0 echo   No inline policy found
%AWSCMD% iam delete-role --role-name %ROLE_NAME% 2>nul
if %errorlevel% neq 0 echo   Role not found
echo   IAM role deleted

REM --- Delete S3 bucket ---
echo.
echo [7/7] Deleting S3 bucket: %BUCKET_NAME%
%AWSCMD% s3 rb s3://%BUCKET_NAME% --force 2>nul
if %errorlevel% neq 0 echo   Bucket not found or not empty

REM --- Done ---
echo.
echo ============================================
echo  Teardown Complete!
echo ============================================

endlocal
