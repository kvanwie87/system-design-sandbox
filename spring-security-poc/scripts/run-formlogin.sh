#!/usr/bin/env bash
# Start the application with the "formlogin" profile.
# All endpoints require authentication via the default login form.
set -euo pipefail

source "$(dirname "${BASH_SOURCE[0]}")/lib-common.sh"
ensure_java_home

cd "${PROJECT_ROOT}"
echo "Starting app with profile: formlogin"
echo "Log in at ${BASE_URL}/hello with ${APP_USER} / ${APP_PASSWORD}"
exec ./gradlew bootRun --args='--spring.profiles.active=formlogin'
