#!/usr/bin/env bash
# Start the application with the "permitall" profile.
# All endpoints are open — no authentication required.
set -euo pipefail

source "$(dirname "${BASH_SOURCE[0]}")/lib-common.sh"
ensure_java_home

cd "${PROJECT_ROOT}"
echo "Starting app with profile: permitall"
echo "All endpoints open, e.g. ${BASE_URL}/hello"
exec ./gradlew bootRun --args='--spring.profiles.active=permitall'
