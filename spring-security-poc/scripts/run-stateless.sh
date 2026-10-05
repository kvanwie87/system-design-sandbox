#!/usr/bin/env bash
# Start the application with the "stateless" profile.
# HTTP Basic auth with SessionCreationPolicy.STATELESS (no HttpSession).
set -euo pipefail

source "$(dirname "${BASH_SOURCE[0]}")/lib-common.sh"
ensure_java_home

cd "${PROJECT_ROOT}"
echo "Starting app with profile: stateless"
echo "Every request must carry credentials; no session cookie is issued."
echo "  curl -u ${APP_USER}:${APP_PASSWORD} ${BASE_URL}/hello"
exec ./gradlew bootRun --args='--spring.profiles.active=stateless'
