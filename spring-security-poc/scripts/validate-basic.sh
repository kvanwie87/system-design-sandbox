#!/usr/bin/env bash
# Validate the "basic" profile (HTTP Basic auth).
#
# Expectations for GET /hello:
#   1. No credentials        -> 401 Unauthorized
#   2. Valid credentials     -> 200 "Hello, World!"
#   3. Wrong credentials     -> 401 Unauthorized
#
# Run the app first:  ./scripts/run-basic.sh
set -euo pipefail

source "$(dirname "${BASH_SOURCE[0]}")/lib-common.sh"

URL="${BASE_URL}/hello"
pass=0

# --- Check 1: no credentials -> 401 ----------------------------------------
echo "Check 1: GET ${URL} with no credentials -> expect 401"
status="$(curl -s -o /dev/null -w '%{http_code}' "${URL}")"
if [ "${status}" = "401" ]; then
	echo "  PASS: got 401"
else
	echo "  FAIL: expected 401, got ${status}" >&2
	pass=1
fi

# --- Check 2: valid credentials -> 200 + body ------------------------------
echo "Check 2: GET ${URL} with ${APP_USER}:${APP_PASSWORD} -> expect 200"
body="$(curl -s -u "${APP_USER}:${APP_PASSWORD}" "${URL}")"
status="$(curl -s -o /dev/null -w '%{http_code}' -u "${APP_USER}:${APP_PASSWORD}" "${URL}")"
if [ "${status}" = "200" ] && [ "${body}" = "Hello, World!" ]; then
	echo "  PASS: got 200 '${body}'"
else
	echo "  FAIL: expected 200 'Hello, World!', got ${status} '${body}'" >&2
	pass=1
fi

# --- Check 3: wrong credentials -> 401 -------------------------------------
echo "Check 3: GET ${URL} with wrong password -> expect 401"
status="$(curl -s -o /dev/null -w '%{http_code}' -u "${APP_USER}:wrong-password" "${URL}")"
if [ "${status}" = "401" ]; then
	echo "  PASS: got 401"
else
	echo "  FAIL: expected 401, got ${status}" >&2
	pass=1
fi

exit "${pass}"
