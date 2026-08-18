#!/bin/bash
# Start everything: infrastructure + all application services (containerized)

echo -e "\033[36mBuilding and starting full stack (infra + apps)...\033[0m"
docker compose --profile apps up --build -d
echo ""
echo -e "\033[32mFull stack started. Services available at:\033[0m"
echo "  Product Service   : http://localhost:8081"
echo "  Search Service    : http://localhost:8082"
echo "  Cart Service      : http://localhost:8083"
echo "  Order Service     : http://localhost:8084"
echo "  Inventory Service : http://localhost:8085"
echo "  Payment Service   : http://localhost:8086"
echo ""
echo "  PostgreSQL    : localhost:5432"
echo "  Redis         : localhost:6379"
echo "  Elasticsearch : localhost:9200"
echo "  Kafka         : localhost:19092"
echo ""
echo "Run E2E tests with: ./e2e-test.ps1"
