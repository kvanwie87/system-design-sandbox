#!/usr/bin/env bash
# Validate the "jwt" profile (stateless RS256 bearer tokens).
#
# Expectations:
#   1. GET /hello with no token            -> 401
#   2. POST /login with valid creds        -> 200 + a JWT
#   3. GET /hello with the Bearer token    -> 200 "Hello, World!"
#   4. POST /login with wrong password     -> 401
#   5. GET /hello with a tampered token    -> 401
#
# Run the app first:  ./scripts/run-jwt.sh
set -euo pipefail

source "$(dirname "${BASH_SOURCE[0]}")/lib-common.sh"

HELLO_URL="${BASE_URL}/hello"
LOGIN_URL="${BASE_URL}/login"
pass=0

login() { # args: username password  -> prints HTTP status to stderr, body to stdout
	curl -s -w '\n%{http_code}' -X POST -H 'Content-Type: application/json' \
		-d "{\"username\":\"$1\",\"password\":\"$2\"}" "${LOGIN_URL}"
}

# --- Check 1: no token -> 401 ----------------------------------------------
echo "Check 1: GET ${HELLO_URL} with no token -> expect 401"
status="$(curl -s -o /dev/null -w '%{http_code}' "${HELLO_URL}")"
if [ "${status}" = "401" ]; then echo "  PASS: got 401"; else echo "  FAIL: expected 401, got ${status}" >&2; pass=1; fi

# --- Check 2: login -> 200 + token -----------------------------------------
echo "Check 2: POST ${LOGIN_URL} with ${APP_USER}/${APP_PASSWORD} -> expect 200 + token"
resp="$(login "${APP_USER}" "${APP_PASSWORD}")"
login_status="$(printf '%s' "${resp}" | tail -1)"
login_body="$(printf '%s' "${resp}" | sed '$d')"
token="$(printf '%s' "${login_body}" | sed -E 's/.*"token":"([^"]*)".*/\1/')"
if [ "${login_status}" = "200" ] && [ -n "${token}" ] && [ "${token}" != "${login_body}" ]; then
	echo "  PASS: got token (${token:0:24}...)"
else
	echo "  FAIL: expected 200 + token, got ${login_status} ${login_body}" >&2; pass=1; token=""
fi

# --- Check 3: use token -> 200 ---------------------------------------------
echo "Check 3: GET ${HELLO_URL} with Bearer token -> expect 200"
if [ -z "${token}" ]; then
	echo "  SKIP: no token from check 2" >&2; pass=1
else
	body="$(curl -s -H "Authorization: Bearer ${token}" "${HELLO_URL}")"
	if [ "${body}" = "Hello, World!" ]; then echo "  PASS: '${body}'"; else echo "  FAIL: got '${body}'" >&2; pass=1; fi
fi

# --- Check 4: wrong password -> 401 ----------------------------------------
echo "Check 4: POST ${LOGIN_URL} with wrong password -> expect 401"
resp="$(login "${APP_USER}" "definitely-wrong")"
status="$(printf '%s' "${resp}" | tail -1)"
if [ "${status}" = "401" ]; then echo "  PASS: got 401"; else echo "  FAIL: expected 401, got ${status}" >&2; pass=1; fi

# --- Check 5: tampered token -> 401 ----------------------------------------
echo "Check 5: GET ${HELLO_URL} with a tampered token -> expect 401"
if [ -z "${token}" ]; then
	echo "  SKIP: no token to tamper" >&2
else
	# Flip the last character of the signature to break it.
	tampered="${token%?}X"
	status="$(curl -s -o /dev/null -w '%{http_code}' -H "Authorization: Bearer ${tampered}" "${HELLO_URL}")"
	if [ "${status}" = "401" ]; then echo "  PASS: got 401 (signature check rejected it)"; else echo "  FAIL: expected 401, got ${status}" >&2; pass=1; fi
fi

exit "${pass}"
