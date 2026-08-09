@echo off
REM ============================================================
REM LocalStack Setup Script (Windows - Step Functions version)
REM Sets up S3 buckets, deploys Lambda functions, creates the
REM Step Functions state machine, and configures the S3 trigger.
REM ============================================================

setlocal enabledelayedexpansion

set ENDPOINT=http://localhost:4566
set INPUT_BUCKET=csv-input-bucket
set OUTPUT_BUCKET=csv-output-bucket
set REGION=us-east-1
set JAR_PATH=build\libs\java-lambda-poc-all.jar
set STATE_MACHINE_NAME=csv-pipeline

REM Lambda function names
set TRIGGER_FUNCTION=csv-trigger
set DOWNLOAD_FUNCTION=csv-download
set FILTER_FUNCTION=csv-filter
set ENRICH_FUNCTION=csv-enrich
set OUTPUT_FUNCTION=csv-output

REM Handlers
set TRIGGER_HANDLER=com.example.lambda.StepFunctionTriggerHandler
set DOWNLOAD_HANDLER=com.example.lambda.steps.DownloadHandler
set FILTER_HANDLER=com.example.lambda.steps.FilterHandler
set ENRICH_HANDLER=com.example.lambda.steps.EnrichHandler
set OUTPUT_HANDLER=com.example.lambda.steps.OutputHandler

set AWS_CMD=aws --endpoint-url=%ENDPOINT% --region %REGION%

REM Disable trailing checksums (AWS CLI v2 feature not supported by LocalStack v2)
set AWS_REQUEST_CHECKSUM_CALCULATION=WHEN_REQUIRED
set AWS_RESPONSE_CHECKSUM_VALIDATION=WHEN_REQUIRED

echo ============================================
echo  LocalStack Step Functions POC Setup
echo ============================================

REM Check if JAR exists
if not exist "%JAR_PATH%" (
    echo.
    echo ERROR: Shadow JAR not found at %JAR_PATH%
    echo Run 'gradlew.bat shadowJar' first.
    exit /b 1
)

echo.
echo --- Creating S3 buckets ---
%AWS_CMD% s3 mb s3://%INPUT_BUCKET% 2>nul
%AWS_CMD% s3 mb s3://%OUTPUT_BUCKET% 2>nul
echo Created: %INPUT_BUCKET%, %OUTPUT_BUCKET%

echo.
echo --- Deploying step Lambda functions ---

REM Deploy Download function
%AWS_CMD% lambda delete-function --function-name %DOWNLOAD_FUNCTION% 2>nul
%AWS_CMD% lambda create-function ^
    --function-name %DOWNLOAD_FUNCTION% ^
    --runtime java17 ^
    --handler %DOWNLOAD_HANDLER% ^
    --role arn:aws:iam::000000000000:role/lambda-role ^
    --zip-file fileb://%JAR_PATH% ^
    --timeout 60 --memory-size 512 ^
    --environment "Variables={AWS_REGION=%REGION%}" ^
    --no-cli-pager
echo   Deployed: %DOWNLOAD_FUNCTION%

REM Deploy Filter function
%AWS_CMD% lambda delete-function --function-name %FILTER_FUNCTION% 2>nul
%AWS_CMD% lambda create-function ^
    --function-name %FILTER_FUNCTION% ^
    --runtime java17 ^
    --handler %FILTER_HANDLER% ^
    --role arn:aws:iam::000000000000:role/lambda-role ^
    --zip-file fileb://%JAR_PATH% ^
    --timeout 60 --memory-size 512 ^
    --no-cli-pager
echo   Deployed: %FILTER_FUNCTION%

REM Deploy Enrich function
%AWS_CMD% lambda delete-function --function-name %ENRICH_FUNCTION% 2>nul
%AWS_CMD% lambda create-function ^
    --function-name %ENRICH_FUNCTION% ^
    --runtime java17 ^
    --handler %ENRICH_HANDLER% ^
    --role arn:aws:iam::000000000000:role/lambda-role ^
    --zip-file fileb://%JAR_PATH% ^
    --timeout 60 --memory-size 512 ^
    --no-cli-pager
echo   Deployed: %ENRICH_FUNCTION%

REM Deploy Output function
%AWS_CMD% lambda delete-function --function-name %OUTPUT_FUNCTION% 2>nul
%AWS_CMD% lambda create-function ^
    --function-name %OUTPUT_FUNCTION% ^
    --runtime java17 ^
    --handler %OUTPUT_HANDLER% ^
    --role arn:aws:iam::000000000000:role/lambda-role ^
    --zip-file fileb://%JAR_PATH% ^
    --timeout 60 --memory-size 512 ^
    --environment "Variables={AWS_REGION=%REGION%}" ^
    --no-cli-pager
echo   Deployed: %OUTPUT_FUNCTION%

echo.
echo --- Waiting for Lambda functions to be active ---
timeout /t 5 /nobreak >nul

REM Get Lambda ARNs
echo.
echo --- Getting Lambda ARNs ---
for /f "tokens=*" %%a in ('%AWS_CMD% lambda get-function --function-name %DOWNLOAD_FUNCTION% --query Configuration.FunctionArn --output text') do set DOWNLOAD_ARN=%%a
for /f "tokens=*" %%a in ('%AWS_CMD% lambda get-function --function-name %FILTER_FUNCTION% --query Configuration.FunctionArn --output text') do set FILTER_ARN=%%a
for /f "tokens=*" %%a in ('%AWS_CMD% lambda get-function --function-name %ENRICH_FUNCTION% --query Configuration.FunctionArn --output text') do set ENRICH_ARN=%%a
for /f "tokens=*" %%a in ('%AWS_CMD% lambda get-function --function-name %OUTPUT_FUNCTION% --query Configuration.FunctionArn --output text') do set OUTPUT_ARN=%%a

echo   Download: %DOWNLOAD_ARN%
echo   Filter:   %FILTER_ARN%
echo   Enrich:   %ENRICH_ARN%
echo   Output:   %OUTPUT_ARN%

echo.
echo --- Creating Step Functions state machine ---

REM Build state machine definition with actual ARNs
set SM_DEF={\"Comment\":\"CSV Processing Pipeline\",\"StartAt\":\"DownloadAndParse\",\"States\":{\"DownloadAndParse\":{\"Type\":\"Task\",\"Resource\":\"%DOWNLOAD_ARN%\",\"Next\":\"CheckDownloadStatus\"},\"CheckDownloadStatus\":{\"Type\":\"Choice\",\"Choices\":[{\"Variable\":\"$.status\",\"StringEquals\":\"ERROR\",\"Next\":\"ProcessingFailed\"},{\"Variable\":\"$.status\",\"StringEquals\":\"EMPTY\",\"Next\":\"EmptyFile\"}],\"Default\":\"FilterRows\"},\"FilterRows\":{\"Type\":\"Task\",\"Resource\":\"%FILTER_ARN%\",\"Next\":\"EnrichRows\"},\"EnrichRows\":{\"Type\":\"Task\",\"Resource\":\"%ENRICH_ARN%\",\"Next\":\"WriteOutput\"},\"WriteOutput\":{\"Type\":\"Task\",\"Resource\":\"%OUTPUT_ARN%\",\"Next\":\"CheckOutputStatus\"},\"CheckOutputStatus\":{\"Type\":\"Choice\",\"Choices\":[{\"Variable\":\"$.status\",\"StringEquals\":\"ERROR\",\"Next\":\"ProcessingFailed\"}],\"Default\":\"ProcessingComplete\"},\"ProcessingComplete\":{\"Type\":\"Succeed\"},\"EmptyFile\":{\"Type\":\"Succeed\"},\"ProcessingFailed\":{\"Type\":\"Fail\",\"Cause\":\"Processing error\",\"Error\":\"ProcessingError\"}}}

for /f "tokens=*" %%a in ('%AWS_CMD% stepfunctions create-state-machine --name %STATE_MACHINE_NAME% --definition "%SM_DEF%" --role-arn arn:aws:iam::000000000000:role/stepfunctions-role --query stateMachineArn --output text') do set STATE_MACHINE_ARN=%%a

echo State machine created: %STATE_MACHINE_ARN%

echo.
echo --- Deploying trigger Lambda ---
%AWS_CMD% lambda delete-function --function-name %TRIGGER_FUNCTION% 2>nul
%AWS_CMD% lambda create-function ^
    --function-name %TRIGGER_FUNCTION% ^
    --runtime java17 ^
    --handler %TRIGGER_HANDLER% ^
    --role arn:aws:iam::000000000000:role/lambda-role ^
    --zip-file fileb://%JAR_PATH% ^
    --timeout 60 --memory-size 512 ^
    --environment "Variables={STATE_MACHINE_ARN=%STATE_MACHINE_ARN%,OUTPUT_BUCKET=%OUTPUT_BUCKET%,AWS_REGION=%REGION%}" ^
    --no-cli-pager
echo   Deployed: %TRIGGER_FUNCTION%

echo.
echo --- Waiting for trigger Lambda ---
timeout /t 3 /nobreak >nul

echo.
echo --- Adding S3 invoke permission ---
%AWS_CMD% lambda add-permission ^
    --function-name %TRIGGER_FUNCTION% ^
    --statement-id s3-trigger ^
    --action lambda:InvokeFunction ^
    --principal s3.amazonaws.com ^
    --source-arn arn:aws:s3:::%INPUT_BUCKET% ^
    --no-cli-pager

echo.
echo --- Configuring S3 trigger ---

for /f "tokens=*" %%a in ('%AWS_CMD% lambda get-function --function-name %TRIGGER_FUNCTION% --query Configuration.FunctionArn --output text') do set TRIGGER_ARN=%%a

echo {"LambdaFunctionConfigurations":[{"LambdaFunctionArn":"%TRIGGER_ARN%","Events":["s3:ObjectCreated:*"],"Filter":{"Key":{"FilterRules":[{"Name":"suffix","Value":".csv"}]}}}]} > %TEMP%\notification-config.json

%AWS_CMD% s3api put-bucket-notification-configuration ^
    --bucket %INPUT_BUCKET% ^
    --notification-configuration file://%TEMP%\notification-config.json

del %TEMP%\notification-config.json
echo S3 trigger configured: %INPUT_BUCKET% -^> %TRIGGER_FUNCTION% -^> %STATE_MACHINE_NAME%

echo.
echo ============================================
echo  Setup complete!
echo ============================================
echo.
echo To test, run: scripts\send-sample.bat
