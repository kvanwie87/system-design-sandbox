#!/bin/bash
set -e

echo "Starting full stack (Redis + App)..."
docker compose --profile app up -d --build

echo "Waiting for Redis to be healthy..."
until docker compose exec redis redis-cli ping 2>/dev/null | grep -q PONG; do
  sleep 1
done
echo "Redis is ready."

echo "Waiting for app to be ready..."
for i in $(seq 1 60); do
  if curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/api/cache/healthping 2>/dev/null | grep -q "200"; then
    echo "Stack is ready! App: http://localhost:8080"
    exit 0
  fi
  sleep 2
done

echo "WARNING: App did not become ready within 120 seconds. Check logs with: docker compose logs app"
exit 1
