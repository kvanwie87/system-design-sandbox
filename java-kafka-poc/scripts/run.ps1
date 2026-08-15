<#
.SYNOPSIS
    Manages the Kafka PoC Docker Compose stack (Windows).

.DESCRIPTION
    Starts infrastructure (Kafka, Schema Registry, PostgreSQL) with or without
    the Spring Boot application containers.

.PARAMETER Command
    up      - Start services (full stack with apps)
    down    - Stop and remove all services
    infra   - Start infrastructure only (no Spring Boot apps)
    build   - Build app JARs and Docker images
    logs    - Tail logs for all running services
    status  - Show running containers

.EXAMPLE
    .\scripts\run.ps1 up        # Full stack: infra + apps
    .\scripts\run.ps1 infra     # Infrastructure only
    .\scripts\run.ps1 down      # Tear everything down
    .\scripts\run.ps1 build     # Build JARs then Docker images
    .\scripts\run.ps1 logs      # Tail logs
    .\scripts\run.ps1 status    # Show container status
#>

param(
    [Parameter(Position = 0)]
    [ValidateSet("up", "down", "infra", "build", "logs", "status")]
    [string]$Command = "up"
)

$ErrorActionPreference = "Stop"
$ProjectRoot = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
Push-Location $ProjectRoot

try {
    switch ($Command) {
        "up" {
            Write-Host "Starting full stack (infra + apps)..." -ForegroundColor Cyan
            docker compose --profile app up -d --build
        }
        "infra" {
            Write-Host "Starting infrastructure only (Kafka, Schema Registry, PostgreSQL)..." -ForegroundColor Cyan
            docker compose up -d
            Write-Host ""
            Write-Host "Infrastructure is up. Run apps locally with:" -ForegroundColor Green
            Write-Host "  .\gradlew.bat :sensor-ingest:bootRun"
            Write-Host "  .\gradlew.bat :telemetry-processor:bootRun"
        }
        "down" {
            Write-Host "Stopping all services..." -ForegroundColor Yellow
            docker compose --profile app down -v
        }
        "build" {
            Write-Host "Building application JARs..." -ForegroundColor Cyan
            & .\gradlew.bat :sensor-ingest:bootJar :telemetry-processor:bootJar --no-daemon --console=plain
            Write-Host "Building Docker images..." -ForegroundColor Cyan
            docker compose --profile app build
            Write-Host "Done. Run '.\scripts\run.ps1 up' to start." -ForegroundColor Green
        }
        "logs" {
            docker compose --profile app logs -f
        }
        "status" {
            docker compose --profile app ps
        }
    }
}
finally {
    Pop-Location
}
