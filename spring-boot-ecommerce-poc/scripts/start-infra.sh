#!/bin/bash
# Start infrastructure only (PostgreSQL, Redis, Elasticsearch, Kafka)

echo -e "\033[36mStarting infrastructure containers...\033[0m"
docker compose up -d
echo ""
echo -e "\033[32mInfrastructure started. Services available at:\033[0m"
echo "  PostgreSQL    : localhost:5432  (ecommerce/ecommerce)"
echo "  Redis         : localhost:6379"
echo "  Elasticsearch : localhost:9200"
echo "  Kafka         : localhost:19092"
echo ""
echo "Run Spring Boot services locally with: ./gradlew :<service>:bootRun"
