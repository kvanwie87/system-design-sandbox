@echo off
REM ============================================================
REM Local test script for Windows: Builds the project and runs
REM the CSV processor against sample data to verify output.
REM ============================================================

setlocal

set SCRIPT_DIR=%~dp0
set PROJECT_DIR=%SCRIPT_DIR%..

echo === Building shadow JAR ===
cd /d "%PROJECT_DIR%"
call gradlew.bat shadowJar --quiet
if %ERRORLEVEL% neq 0 (
    echo Build failed!
    exit /b 1
)

echo.
echo === Running CSV processor against sample data ===
java -cp build\libs\java-lambda-poc-all.jar com.example.lambda.LocalTestRunner sample-data\orders.csv build\test-output.json
if %ERRORLEVEL% neq 0 (
    echo Processing failed!
    exit /b 1
)

echo.
echo === Output ===
type build\test-output.json

echo.
echo.
echo === Test complete. Check build\test-output.json against sample-data\expected-output.json ===
