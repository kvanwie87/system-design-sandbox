# Start everything: infrastructure + all application services (containerized)
# Uses the "apps" profile to include the 6 Spring Boot services.

Write-Host "Building and starting full stack (infra + apps)..." -ForegroundColor Cyan
docker compose --profile apps up --build -d
Write-Host ""
Write-Host "Full stack started. Services available at:" -ForegroundColor Green
Write-Host "  Product Service   : http://localhost:8081"
Write-Host "  Search Service    : http://localhost:8082"
Write-Host "  Cart Service      : http://localhost:8083"
Write-Host "  Order Service     : http://localhost:8084"
Write-Host "  Inventory Service : http://localhost:8085"
Write-Host "  Payment Service   : http://localhost:8086"
Write-Host ""
Write-Host "  PostgreSQL : localhost:5432"
Write-Host "  Redis      : localhost:6379"
Write-Host "  Elasticsearch: localhost:9200"
Write-Host "  Kafka      : localhost:19092"
Write-Host ""
Write-Host "Run E2E tests with: .\e2e-test.ps1"
