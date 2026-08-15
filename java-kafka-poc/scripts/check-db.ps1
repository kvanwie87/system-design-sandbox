<#
.SYNOPSIS
    Validates that sensor readings are being persisted to PostgreSQL (Windows).

.PARAMETER Command
    summary  - Total count + latest 10 readings (default)
    count    - Just print the total record count
    watch    - Poll every 2s showing new record counts
    sensors  - Per-sensor breakdown with averages
    alerts   - Show readings that exceeded temperature threshold

.EXAMPLE
    .\scripts\check-db.ps1              # Summary + latest 10
    .\scripts\check-db.ps1 count        # Just the total count
    .\scripts\check-db.ps1 watch        # Poll every 2 seconds
    .\scripts\check-db.ps1 sensors      # Per-sensor breakdown
    .\scripts\check-db.ps1 alerts       # Show alert-level readings
#>

param(
    [Parameter(Position = 0)]
    [ValidateSet("summary", "count", "watch", "sensors", "alerts")]
    [string]$Command = "summary"
)

$ErrorActionPreference = "Stop"

function Invoke-Psql {
    param([string]$Query, [switch]$Pretty)
    if ($Pretty) {
        docker exec -i postgres psql -U telemetry -d telemetry -c $Query
    } else {
        docker exec -i postgres psql -U telemetry -d telemetry -t -A -c $Query
    }
}

function Test-Connection {
    $result = docker exec postgres pg_isready -U telemetry 2>$null
    if ($LASTEXITCODE -ne 0) {
        Write-Host "ERROR: Cannot connect to PostgreSQL. Is the container running?" -ForegroundColor Red
        Write-Host "  Run: docker compose up -d postgres"
        exit 1
    }
}

switch ($Command) {
    "summary" {
        Test-Connection
        Write-Host "=== Database Validation ===" -ForegroundColor Cyan
        Write-Host ""
        $total = (Invoke-Psql "SELECT COUNT(*) FROM sensor_readings;").Trim()
        Write-Host "Total readings persisted: $total"
        Write-Host ""
        if ([int]$total -gt 0) {
            $sensors = (Invoke-Psql "SELECT COUNT(DISTINCT sensor_id) FROM sensor_readings;").Trim()
            $first = (Invoke-Psql "SELECT MIN(timestamp) FROM sensor_readings;").Trim()
            $last = (Invoke-Psql "SELECT MAX(timestamp) FROM sensor_readings;").Trim()
            Write-Host "Distinct sensors: $sensors"
            Write-Host "First reading:    $first"
            Write-Host "Latest reading:   $last"
            Write-Host ""
            Write-Host "--- Latest 10 readings ---" -ForegroundColor Gray
            Invoke-Psql "SELECT id, sensor_id, temperature, humidity, location, kafka_partition, kafka_offset, timestamp FROM sensor_readings ORDER BY id DESC LIMIT 10;" -Pretty
        } else {
            Write-Host "No readings found. Is the pipeline running?" -ForegroundColor Yellow
            Write-Host "  1. Start infra:   .\scripts\run.ps1 infra"
            Write-Host "  2. Start apps:    .\gradlew.bat :sensor-ingest:bootRun / :telemetry-processor:bootRun"
            Write-Host "  3. Send data:     .\scripts\generate-data.ps1 -Count 5"
        }
    }
    "count" {
        Test-Connection
        $total = (Invoke-Psql "SELECT COUNT(*) FROM sensor_readings;").Trim()
        Write-Host $total
    }
    "watch" {
        Test-Connection
        Write-Host "Watching database for new records (Ctrl+C to stop)..." -ForegroundColor Cyan
        Write-Host ""
        $prev = 0
        while ($true) {
            $total = [int](Invoke-Psql "SELECT COUNT(*) FROM sensor_readings;").Trim()
            $diff = $total - $prev
            if ($diff -gt 0) {
                $ts = Get-Date -Format "HH:mm:ss"
                Write-Host "[$ts] Total: $total (+$diff new)" -ForegroundColor Green
            }
            $prev = $total
            Start-Sleep -Seconds 2
        }
    }
    "sensors" {
        Test-Connection
        Write-Host "=== Per-Sensor Breakdown ===" -ForegroundColor Cyan
        Write-Host ""
        Invoke-Psql "SELECT sensor_id, COUNT(*) as readings, ROUND(AVG(temperature)::numeric, 1) as avg_temp, ROUND(AVG(humidity)::numeric, 1) as avg_humidity, MAX(timestamp) as last_reading FROM sensor_readings GROUP BY sensor_id ORDER BY readings DESC;" -Pretty
    }
    "alerts" {
        Test-Connection
        Write-Host "=== Readings Above Alert Threshold (35C) ===" -ForegroundColor Cyan
        Write-Host ""
        Invoke-Psql "SELECT id, sensor_id, temperature, humidity, location, timestamp FROM sensor_readings WHERE temperature > 35.0 ORDER BY timestamp DESC LIMIT 20;" -Pretty
        $alertCount = (Invoke-Psql "SELECT COUNT(*) FROM sensor_readings WHERE temperature > 35.0;").Trim()
        Write-Host "Total alert-level readings: $alertCount" -ForegroundColor Yellow
    }
}
