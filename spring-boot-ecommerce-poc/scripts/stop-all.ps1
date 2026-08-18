# Stop all containers (infrastructure + apps) and remove orphans

Write-Host "Stopping all containers..." -ForegroundColor Yellow
docker compose --profile apps down
Write-Host "All containers stopped." -ForegroundColor Green
