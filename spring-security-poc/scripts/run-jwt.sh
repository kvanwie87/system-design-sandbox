#!/usr/bin/env bash
# Start the application with the "jwt" profile.
# Stateless RS256 bearer-token auth: POST /login for a token, then send it as
# Authorization: Bearer <token>.
set -euo pipefail

source "$(dirname "${BASH_SOURCE[0]}")/lib-common.sh"
ensure_java_home

cd "${PROJECT_ROOT}"
echo "Starting app with profile: jwt"
echo "Get a token, then call a protected endpoint:"
echo "  TOKEN=\$(curl -s -X POST -H 'Content-Type: application/json' \\"
echo "       -d '{\"username\":\"${APP_USER}\",\"password\":\"${APP_PASSWORD}\"}' \\"
echo "       ${BASE_URL}/login | sed -E 's/.*\"token\":\"([^\"]*)\".*/\\1/')"
echo "  curl -H \"Authorization: Bearer \$TOKEN\" ${BASE_URL}/hello"
exec ./gradlew bootRun --args='--spring.profiles.active=jwt'
