@echo off
REM ============================================================
REM Runs both test scripts back-to-back.
REM Both modules must be running:
REM   gradlew.bat :spring-threading:bootRun   (port 8080)
REM   gradlew.bat :custom-threading:bootRun   (port 8081)
REM ============================================================

echo Running spring-threading tests...
echo.
call "%~dp0test-spring-threading.bat"

echo.
echo.
echo ============================================
echo.
echo.

echo Running custom-threading tests...
echo.
call "%~dp0test-custom-threading.bat"
