#!/usr/bin/env bash
# Validate the "stateless" profile (HTTP Basic auth, no session).
#
# Expectations for GET /hello:
#   1. No credentials     -> 401
#   2. Valid credentials  -> 200 "Hello, World!"
#   3. A successful response sets NO JSESSIONID cookie (the point of STATELESS).
#
# Run the app first:  ./scripts/run-stateless.sh
set -euo pipefail

source "$(dirname "${BASH_SOURCE[0]}")/lib-common.sh"

URL="${BASE_URL}/hello"
AUTH=(-u "${APP_USER}:${APP_PASSWORD}")
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
echo "Check 2: GET ${URL} with valid creds -> expect 200"
body="$(curl -s "${AUTH[@]}" "${URL}")"
status="$(curl -s -o /dev/null -w '%{http_code}' "${AUTH[@]}" "${URL}")"
if [ "${status}" = "200" ] && [ "${body}" = "Hello, World!" ]; then
	echo "  PASS: got 200 '${body}'"
else
	echo "  FAIL: expected 200 'Hello, World!', got ${status} '${body}'" >&2
	pass=1
fi

# --- Check 3: no JSESSIONID cookie on a successful response -----------------
echo "Check 3: successful GET ${URL} should NOT set a JSESSIONID cookie"
set_cookie="$(curl -s -D - -o /dev/null "${AUTH[@]}" "${URL}" \
	| grep -i '^set-cookie:' | grep -i 'JSESSIONID' || true)"
if [ -z "${set_cookie}" ]; then
	echo "  PASS: no JSESSIONID set (session is stateless)"
else
	echo "  FAIL: a session cookie was set -> ${set_cookie}" >&2
	pass=1
fi

exit "${pass}"
