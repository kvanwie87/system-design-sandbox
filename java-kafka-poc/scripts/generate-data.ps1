<#
.SYNOPSIS
    Sends randomized sensor readings to the sensor-ingest service (Windows).

.PARAMETER BaseUrl
    Base URL of the sensor-ingest service. Default: http://localhost:8080

.PARAMETER IntervalMs
    Milliseconds between requests. Default: 1000

.PARAMETER Count
    Number of readings to send. Default: 0 (infinite until Ctrl+C)

.PARAMETER Sensors
    Number of simulated sensors. Default: 5

.EXAMPLE
    .\scripts\generate-data.ps1                          # Default: 5 sensors, 1s interval, infinite
    .\scripts\generate-data.ps1 -Count 50 -IntervalMs 500
    .\scripts\generate-data.ps1 -Sensors 10 -IntervalMs 200
#>

param(
    [string]$BaseUrl = "http://localhost:8080",
    [int]$IntervalMs = 1000,
    [int]$Count = 0,
    [int]$Sensors = 5
)

$locations = @("warehouse-A", "warehouse-B", "server-room", "rooftop", "basement", "lab-1", "lab-2", "cold-storage", "furnace-room", "outdoor-north")
$sensorIds = 1..$Sensors | ForEach-Object { "sensor-{0:D3}" -f $_ }

function Get-RandomReading {
    $sensorId = $sensorIds | Get-Random
    $location = $locations | Get-Random
    # Temperature: mostly 15-30, occasionally spikes above 35 (triggers alerts)
    $temp = if ((Get-Random -Minimum 1 -Maximum 100) -le 15) {
        [math]::Round((Get-Random -Minimum 350 -Maximum 550) / 10.0, 1)
    } else {
        [math]::Round((Get-Random -Minimum 150 -Maximum 300) / 10.0, 1)
    }
    $humidity = [math]::Round((Get-Random -Minimum 200 -Maximum 900) / 10.0, 1)
    $batteryLevel = if ((Get-Random -Minimum 1 -Maximum 100) -le 40) {
        [math]::Round((Get-Random -Minimum 100 -Maximum 1000) / 10.0, 1)
    } else {
        $null
    }

    $body = @{
        sensorId    = $sensorId
        temperature = $temp
        humidity    = $humidity
        location    = $location
    }
    if ($null -ne $batteryLevel) {
        $body.batteryLevel = $batteryLevel
    }
    return $body
}

Write-Host "Sensor Data Generator" -ForegroundColor Cyan
Write-Host "  Target:   $BaseUrl/api/sensors/readings" -ForegroundColor Gray
Write-Host "  Sensors:  $Sensors" -ForegroundColor Gray
Write-Host "  Interval: ${IntervalMs}ms" -ForegroundColor Gray
Write-Host "  Count:    $(if ($Count -eq 0) { 'infinite (Ctrl+C to stop)' } else { $Count })" -ForegroundColor Gray
Write-Host ""

$sent = 0
$errors = 0

try {
    while ($true) {
        $reading = Get-RandomReading
        $json = $reading | ConvertTo-Json -Compress

        try {
            $response = Invoke-RestMethod -Uri "$BaseUrl/api/sensors/readings" `
                -Method POST -ContentType "application/json" -Body $json -ErrorAction Stop
            $sent++
            $alertFlag = if ($reading.temperature -gt 35) { " [ALERT]" } else { "" }
            Write-Host "[$sent] $($reading.sensorId) | temp=$($reading.temperature)C | humidity=$($reading.humidity)% | $($reading.location)$alertFlag" -ForegroundColor $(if ($reading.temperature -gt 35) { "Red" } else { "Green" })
        }
        catch {
            $errors++
            Write-Host "[$sent] ERROR: $_" -ForegroundColor Red
        }

        if ($Count -gt 0 -and $sent -ge $Count) { break }
        Start-Sleep -Milliseconds $IntervalMs
    }
}
finally {
    Write-Host ""
    Write-Host "Summary: $sent sent, $errors errors" -ForegroundColor Cyan
}
