@echo off
REM Manages the Kafka PoC Docker Compose stack (Windows CMD).
REM
REM Usage:
REM   scripts\run.bat up        Full stack: infra + apps
REM   scripts\run.bat infra     Infrastructure only
REM   scripts\run.bat down      Tear everything down
REM   scripts\run.bat build     Build JARs then Docker images
REM   scripts\run.bat logs      Tail logs
REM   scripts\run.bat status    Show container status

setlocal
pushd "%~dp0\.."

if "%~1"=="" goto :up
if /i "%~1"=="up" goto :up
if /i "%~1"=="infra" goto :infra
if /i "%~1"=="down" goto :down
if /i "%~1"=="build" goto :build
if /i "%~1"=="logs" goto :logs
if /i "%~1"=="status" goto :status
goto :usage

:up
echo Starting full stack (infra + apps)...
docker compose --profile app up -d --build
goto :end

:infra
echo Starting infrastructure only (Kafka, Schema Registry, PostgreSQL)...
docker compose up -d
echo.
echo Infrastructure is up. Run apps locally with:
echo   gradlew.bat :sensor-ingest:bootRun
echo   gradlew.bat :telemetry-processor:bootRun
goto :end

:down
echo Stopping all services...
docker compose --profile app down -v
goto :end

:build
echo Building application JARs...
call gradlew.bat :sensor-ingest:bootJar :telemetry-processor:bootJar --no-daemon --console=plain
echo Building Docker images...
docker compose --profile app build
echo Done. Run 'scripts\run.bat up' to start.
goto :end

:logs
docker compose --profile app logs -f
goto :end

:status
docker compose --profile app ps
goto :end

:usage
echo Usage: scripts\run.bat {up^|infra^|down^|build^|logs^|status}
echo.
echo Commands:
echo   up      Start full stack (infra + apps)
echo   infra   Start infrastructure only (run apps locally)
echo   down    Stop and remove all services
echo   build   Build JARs and Docker images
echo   logs    Tail logs for all services
echo   status  Show running containers
goto :end

:end
popd
endlocal
