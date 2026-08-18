#!/bin/bash
# Stop all containers and remove volumes (full reset)

echo -e "\033[31mStopping all containers and removing volumes...\033[0m"
docker compose --profile apps down -v
echo -e "\033[32mAll containers stopped and volumes removed.\033[0m"
echo "Next start will re-initialize databases from scratch."
