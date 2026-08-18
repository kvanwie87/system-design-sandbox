# Stop all containers and remove volumes (full reset)

Write-Host "Stopping all containers and removing volumes..." -ForegroundColor Red
docker compose --profile apps down -v
Write-Host "All containers stopped and volumes removed." -ForegroundColor Green
Write-Host "Next start will re-initialize databases from scratch."
