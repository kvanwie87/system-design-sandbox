#!/usr/bin/env bash
# Start the application with the "basicwithcsrf" profile.
# HTTP Basic auth with CSRF protection left ON.
set -euo pipefail

source "$(dirname "${BASH_SOURCE[0]}")/lib-common.sh"
ensure_java_home

cd "${PROJECT_ROOT}"
echo "Starting app with profile: basicwithcsrf"
echo "GETs need only Basic auth; POSTs also need a CSRF token."
echo "  curl -u ${APP_USER}:${APP_PASSWORD} ${BASE_URL}/csrf   # fetch token"
exec ./gradlew bootRun --args='--spring.profiles.active=basicwithcsrf'
