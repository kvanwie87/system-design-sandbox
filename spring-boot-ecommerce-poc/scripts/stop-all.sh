#!/bin/bash
# Stop all containers (infrastructure + apps)

echo -e "\033[33mStopping all containers...\033[0m"
docker compose --profile apps down
echo -e "\033[32mAll containers stopped.\033[0m"
