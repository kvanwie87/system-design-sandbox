#!/usr/bin/env bash
# Start the application with the "basic" profile.
# All endpoints require authentication via HTTP Basic.
set -euo pipefail

source "$(dirname "${BASH_SOURCE[0]}")/lib-common.sh"
ensure_java_home

cd "${PROJECT_ROOT}"
echo "Starting app with profile: basic"
echo "Authenticate with HTTP Basic, e.g.:"
echo "  curl -u ${APP_USER}:${APP_PASSWORD} ${BASE_URL}/hello"
exec ./gradlew bootRun --args='--spring.profiles.active=basic'
