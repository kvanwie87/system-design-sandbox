@echo off
setlocal enabledelayedexpansion

REM =============================================================================
REM AWS Infrastructure Setup (Windows)
REM Creates: S3 bucket, SNS topic, 2 SQS queues, IAM role, Lambda, S3 notification
REM Requires: AWS CLI configured with appropriate credentials
REM
REM Usage: setup.bat [--profile <profile>]
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
set JAR_PATH=%~dp0..\..\build\libs\csv-processor-lambda-all.jar
set AWSCMD=aws --region %REGION% %PROFILE_ARG%

REM Get the AWS account ID
for /f "tokens=*" %%i in ('%AWSCMD% sts get-caller-identity --query Account --output text') do set ACCOUNT_ID=%%i
set BUCKET_NAME=csv-input-bucket-%ACCOUNT_ID%

echo ============================================
echo  AWS Infrastructure Setup
echo ============================================
echo  Region:  %REGION%
echo  Account: %ACCOUNT_ID%
echo ============================================

REM --- Create S3 bucket ---
echo.
echo [1/8] Creating S3 bucket: %BUCKET_NAME% (region: %REGION%)
for /f "tokens=*" %%i in ('%AWSCMD% s3api head-bucket --bucket %BUCKET_NAME% --query BucketRegion --output text 2^>nul') do set EXISTING_BUCKET_REGION=%%i
if defined EXISTING_BUCKET_REGION (
    if not "%EXISTING_BUCKET_REGION%"=="%REGION%" (
        echo   ERROR: Bucket already exists in region %EXISTING_BUCKET_REGION%, but script targets %REGION%.
        echo   Delete it first: aws s3 rb s3://%BUCKET_NAME% --force --region %EXISTING_BUCKET_REGION%
        exit /b 1
    )
    echo   Bucket already exists in %REGION%
) else (
    if "%REGION%"=="us-east-1" (
        %AWSCMD% s3api create-bucket --bucket %BUCKET_NAME%
    ) else (
        %AWSCMD% s3api create-bucket --bucket %BUCKET_NAME% --create-bucket-configuration LocationConstraint=%REGION%
    )
    echo   Bucket created
)

REM --- Create SNS topic ---
echo.
echo [2/8] Creating SNS topic: %TOPIC_NAME%
for /f "tokens=*" %%i in ('%AWSCMD% sns create-topic --name %TOPIC_NAME% --query TopicArn --output text') do set TOPIC_ARN=%%i
echo   Topic ARN: %TOPIC_ARN%

REM --- Create SQS queues ---
echo.
echo [3/8] Creating SQS queues: %QUEUE_AUDIT%, %QUEUE_NOTIFICATION%
for /f "tokens=*" %%i in ('%AWSCMD% sqs create-queue --queue-name %QUEUE_AUDIT% --query QueueUrl --output text') do set AUDIT_QUEUE_URL=%%i
for /f "tokens=*" %%i in ('%AWSCMD% sqs create-queue --queue-name %QUEUE_NOTIFICATION% --query QueueUrl --output text') do set NOTIFICATION_QUEUE_URL=%%i
echo   Audit queue URL: %AUDIT_QUEUE_URL%
echo   Notification queue URL: %NOTIFICATION_QUEUE_URL%

set AUDIT_QUEUE_ARN=arn:aws:sqs:%REGION%:%ACCOUNT_ID%:%QUEUE_AUDIT%
set NOTIFICATION_QUEUE_ARN=arn:aws:sqs:%REGION%:%ACCOUNT_ID%:%QUEUE_NOTIFICATION%

REM --- Set SQS policies to allow SNS to send messages ---
echo.
echo [4/8] Setting SQS queue policies for SNS access

REM Generate SQS policy JSON files via PowerShell (cmd.exe cannot handle nested JSON quoting)
powershell -NoProfile -Command "[IO.File]::WriteAllText('%TEMP%\sqs_policy_audit.json', (ConvertTo-Json -Compress -Depth 5 @{QueueUrl='%AUDIT_QUEUE_URL%';Attributes=@{Policy=(ConvertTo-Json -Compress -Depth 5 @{Version='2012-10-17';Statement=@(@{Effect='Allow';Principal=@{Service='sns.amazonaws.com'};Action='sqs:SendMessage';Resource='%AUDIT_QUEUE_ARN%';Condition=@{ArnEquals=@{'aws:SourceArn'='%TOPIC_ARN%'}}})})}}));"
powershell -NoProfile -Command "[IO.File]::WriteAllText('%TEMP%\sqs_policy_notif.json', (ConvertTo-Json -Compress -Depth 5 @{QueueUrl='%NOTIFICATION_QUEUE_URL%';Attributes=@{Policy=(ConvertTo-Json -Compress -Depth 5 @{Version='2012-10-17';Statement=@(@{Effect='Allow';Principal=@{Service='sns.amazonaws.com'};Action='sqs:SendMessage';Resource='%NOTIFICATION_QUEUE_ARN%';Condition=@{ArnEquals=@{'aws:SourceArn'='%TOPIC_ARN%'}}})})}}));"

%AWSCMD% sqs set-queue-attributes --cli-input-json file://%TEMP%\sqs_policy_audit.json
echo   Audit queue policy set
%AWSCMD% sqs set-queue-attributes --cli-input-json file://%TEMP%\sqs_policy_notif.json
echo   Notification queue policy set

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

REM Write assume role policy to temp file via PowerShell
powershell -NoProfile -Command "[IO.File]::WriteAllText('%TEMP%\assume_role_policy.json', (ConvertTo-Json -Compress -Depth 5 @{Version='2012-10-17';Statement=@(@{Effect='Allow';Principal=@{Service='lambda.amazonaws.com'};Action='sts:AssumeRole'})}));"

%AWSCMD% iam create-role --role-name %ROLE_NAME% --assume-role-policy-document file://%TEMP%\assume_role_policy.json --output text >nul 2>&1
if %errorlevel% neq 0 echo   Role already exists

set ROLE_ARN=arn:aws:iam::%ACCOUNT_ID%:role/%ROLE_NAME%
echo   Role ARN: %ROLE_ARN%

REM Write Lambda execution policy to temp file via PowerShell
powershell -NoProfile -Command "[IO.File]::WriteAllText('%TEMP%\lambda_policy.json', (ConvertTo-Json -Compress -Depth 5 @{Version='2012-10-17';Statement=@(@{Effect='Allow';Action=@('logs:CreateLogGroup','logs:CreateLogStream','logs:PutLogEvents');Resource='arn:aws:logs:%REGION%:%ACCOUNT_ID%:*'},@{Effect='Allow';Action=@('s3:GetObject');Resource='arn:aws:s3:::%BUCKET_NAME%/*'},@{Effect='Allow';Action=@('sns:Publish');Resource='%TOPIC_ARN%'})}));"

%AWSCMD% iam put-role-policy --role-name %ROLE_NAME% --policy-name csv-processor-policy --policy-document file://%TEMP%\lambda_policy.json
echo   Inline policy attached
echo   Waiting 10s for IAM role propagation...
timeout /t 10 /nobreak >nul

REM --- Deploy Lambda function ---
echo.
echo [7/8] Deploying Lambda function: %FUNCTION_NAME%
if not exist "%JAR_PATH%" (
    echo ERROR: Shadow JAR not found at %JAR_PATH%
    echo Run 'gradlew shadowJar' first.
    exit /b 1
)

%AWSCMD% lambda get-function --function-name %FUNCTION_NAME% >nul 2>&1
if %errorlevel%==0 (
    echo   Function exists, updating code...
    %AWSCMD% lambda update-function-code --function-name %FUNCTION_NAME% --zip-file "fileb://%JAR_PATH%" --output text >nul
    echo   Waiting for code update to complete...
    %AWSCMD% lambda wait function-updated-v2 --function-name %FUNCTION_NAME% 2>nul
    if %errorlevel% neq 0 timeout /t 10 /nobreak >nul
    %AWSCMD% lambda update-function-configuration --function-name %FUNCTION_NAME% --environment "Variables={SNS_TOPIC_ARN=%TOPIC_ARN%}" --output text >nul
) else (
    %AWSCMD% lambda create-function --function-name %FUNCTION_NAME% --runtime java17 --handler "com.example.processor.CsvProcessorHandler" --role %ROLE_ARN% --zip-file "fileb://%JAR_PATH%" --timeout 60 --memory-size 512 --environment "Variables={SNS_TOPIC_ARN=%TOPIC_ARN%}" --output text >nul
)
echo   Lambda deployed successfully

echo   Waiting for Lambda to become active...
timeout /t 5 /nobreak >nul

REM --- Configure S3 bucket notification ---
echo.
echo [8/8] Configuring S3 bucket notification to trigger Lambda
set LAMBDA_ARN=arn:aws:lambda:%REGION%:%ACCOUNT_ID%:function:%FUNCTION_NAME%

%AWSCMD% lambda add-permission --function-name %FUNCTION_NAME% --statement-id "s3-invoke-%FUNCTION_NAME%" --action "lambda:InvokeFunction" --principal s3.amazonaws.com --source-arn "arn:aws:s3:::%BUCKET_NAME%" --source-account %ACCOUNT_ID% --output text >nul 2>&1
if %errorlevel% neq 0 echo   Permission already exists

powershell -NoProfile -Command "[IO.File]::WriteAllText('%TEMP%\s3_notification.json', (ConvertTo-Json -Compress -Depth 5 @{LambdaFunctionConfigurations=@(@{LambdaFunctionArn='%LAMBDA_ARN%';Events=@('s3:ObjectCreated:*')})}))"

%AWSCMD% s3api put-bucket-notification-configuration --bucket %BUCKET_NAME% --notification-configuration file://%TEMP%\s3_notification.json
echo   S3 notification configured

REM --- Done ---
echo.
echo ============================================
echo  AWS Setup Complete!
echo ============================================
echo.
echo Resources created:
echo   S3 Bucket:    s3://%BUCKET_NAME%
echo   SNS Topic:    %TOPIC_ARN%
echo   SQS Queues:   %AUDIT_QUEUE_URL%
echo                 %NOTIFICATION_QUEUE_URL%
echo   Lambda:       %FUNCTION_NAME%
echo   IAM Role:     %ROLE_ARN%
echo.
echo Test with:
echo   aws s3 cp samples\test.csv s3://%BUCKET_NAME%/
echo   aws sqs receive-message --queue-url %AUDIT_QUEUE_URL% --region %REGION%
echo   aws sqs receive-message --queue-url %NOTIFICATION_QUEUE_URL% --region %REGION%

REM Cleanup temp files
del "%TEMP%\sqs_policy_audit.json" 2>nul
del "%TEMP%\sqs_policy_notif.json" 2>nul
del "%TEMP%\assume_role_policy.json" 2>nul
del "%TEMP%\lambda_policy.json" 2>nul
del "%TEMP%\s3_notification.json" 2>nul

endlocal
