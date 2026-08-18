# Start infrastructure only (PostgreSQL, Redis, Elasticsearch, Kafka)
# Services without the "apps" profile start by default.

Write-Host "Starting infrastructure containers..." -ForegroundColor Cyan
docker compose up -d
Write-Host ""
Write-Host "Infrastructure started. Services available at:" -ForegroundColor Green
Write-Host "  PostgreSQL : localhost:5432  (ecommerce/ecommerce)"
Write-Host "  Redis      : localhost:6379"
Write-Host "  Elasticsearch: localhost:9200"
Write-Host "  Kafka      : localhost:19092"
Write-Host ""
Write-Host "Run Spring Boot services locally with: .\gradlew :<service>:bootRun"
