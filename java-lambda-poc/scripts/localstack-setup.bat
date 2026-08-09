@echo off
REM ============================================================
REM LocalStack Setup Script (Windows)
REM Sets up S3 buckets, deploys the Lambda function, configures
REM S3 event notification trigger, and runs a test upload.
REM ============================================================

setlocal

set ENDPOINT=http://localhost:4566
set INPUT_BUCKET=csv-input-bucket
set OUTPUT_BUCKET=csv-output-bucket
set FUNCTION_NAME=csv-processor
set JAR_PATH=build\libs\java-lambda-poc-all.jar
set HANDLER=com.example.lambda.S3CsvProcessorHandler
set REGION=us-east-1
set AWS_CMD=aws --endpoint-url=%ENDPOINT% --region %REGION%

REM Disable trailing checksums (AWS CLI v2 feature not supported by LocalStack v2)
set AWS_REQUEST_CHECKSUM_CALCULATION=WHEN_REQUIRED
set AWS_RESPONSE_CHECKSUM_VALIDATION=WHEN_REQUIRED

echo ============================================
echo  LocalStack Lambda POC Setup (Windows)
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
echo --- Deploying Lambda function ---
%AWS_CMD% lambda delete-function --function-name %FUNCTION_NAME% 2>nul

%AWS_CMD% lambda create-function ^
    --function-name %FUNCTION_NAME% ^
    --runtime java17 ^
    --handler %HANDLER% ^
    --role arn:aws:iam::000000000000:role/lambda-role ^
    --zip-file fileb://%JAR_PATH% ^
    --timeout 60 ^
    --memory-size 512 ^
    --environment "Variables={OUTPUT_BUCKET=%OUTPUT_BUCKET%,AWS_REGION=%REGION%}"

echo Lambda function '%FUNCTION_NAME%' deployed.

echo.
echo --- Waiting for Lambda to be active ---
timeout /t 3 /nobreak >nul

echo.
echo --- Adding S3 invoke permission to Lambda ---
%AWS_CMD% lambda add-permission ^
    --function-name %FUNCTION_NAME% ^
    --statement-id s3-trigger ^
    --action lambda:InvokeFunction ^
    --principal s3.amazonaws.com ^
    --source-arn arn:aws:s3:::%INPUT_BUCKET%

echo.
echo --- Configuring S3 trigger ---

REM Write notification config to temp file
echo {"LambdaFunctionConfigurations":[{"LambdaFunctionArn":"arn:aws:lambda:%REGION%:000000000000:function:%FUNCTION_NAME%","Events":["s3:ObjectCreated:*"],"Filter":{"Key":{"FilterRules":[{"Name":"suffix","Value":".csv"}]}}}]} > %TEMP%\notification-config.json

%AWS_CMD% s3api put-bucket-notification-configuration ^
    --bucket %INPUT_BUCKET% ^
    --notification-configuration file://%TEMP%\notification-config.json

del %TEMP%\notification-config.json
echo S3 trigger configured: %INPUT_BUCKET% -^> %FUNCTION_NAME% (*.csv files)

echo.
echo ============================================
echo  Setup complete!
echo ============================================
echo.
echo To test, run: scripts\send-sample.bat
