#!/bin/bash
set -e

echo "Stopping all containers..."
docker compose --profile app down -v

echo "Removing dangling images..."
docker image prune -f

echo "Cleanup complete."
