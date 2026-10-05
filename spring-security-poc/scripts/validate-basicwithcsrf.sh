#!/usr/bin/env bash
# Validate the "basicwithcsrf" profile (HTTP Basic auth + CSRF enabled).
#
# Expectations:
#   1. GET /hello with valid creds           -> 200 (GETs are not CSRF-checked)
#   2. POST /echo with valid creds, NO token  -> 401 (see note below)
#   3. POST /echo with valid creds + token    -> 200 (token accepted)
#
# Notes on the status code and the token value (both verified against a live
# server):
#   - A CSRF failure on this chain surfaces as 401, not 403. The filter chain
#     only has HTTP Basic configured, so a denied request is handled by the
#     BasicAuthenticationEntryPoint, which responds 401.
#   - Spring Security 6/7 returns an XOR-ENCODED token from CsrfToken.getToken()
#     (what GET /csrf serves in its JSON "token" field). The raw XSRF-TOKEN
#     cookie holds a DIFFERENT value. The header must carry the ENCODED token
#     from /csrf, not the raw cookie value -- so we parse /csrf's JSON here.
#
# Run the app first:  ./scripts/run-basicwithcsrf.sh
set -euo pipefail

source "$(dirname "${BASH_SOURCE[0]}")/lib-common.sh"

HELLO_URL="${BASE_URL}/hello"
CSRF_URL="${BASE_URL}/csrf"
ECHO_URL="${BASE_URL}/echo"
COOKIE_JAR="$(mktemp)"
trap 'rm -f "${COOKIE_JAR}"' EXIT

AUTH=(-u "${APP_USER}:${APP_PASSWORD}")
pass=0

# --- Check 1: GET /hello with creds -> 200 (not CSRF-checked) ---------------
echo "Check 1: GET ${HELLO_URL} with creds -> expect 200"
status="$(curl -s -o /dev/null -w '%{http_code}' "${AUTH[@]}" "${HELLO_URL}")"
if [ "${status}" = "200" ]; then
	echo "  PASS: got 200"
else
	echo "  FAIL: expected 200, got ${status}" >&2
	pass=1
fi

# --- Check 2: POST /echo with creds, no token -> 401 ------------------------
echo "Check 2: POST ${ECHO_URL} with creds but NO token -> expect 401"
status="$(curl -s -o /dev/null -w '%{http_code}' "${AUTH[@]}" \
	-X POST -H 'Content-Type: text/plain' --data 'hello' "${ECHO_URL}")"
if [ "${status}" = "401" ]; then
	echo "  PASS: got 401 (CSRF blocked the tokenless POST)"
else
	echo "  FAIL: expected 401, got ${status}" >&2
	pass=1
fi

# --- Check 3: POST /echo with creds + token -> 200 --------------------------
echo "Check 3: fetch token from /csrf, then POST ${ECHO_URL} with token -> expect 200"
# GET /csrf returns JSON {"token":"...","headerName":"X-XSRF-TOKEN",...} and
# sets the XSRF-TOKEN cookie. Save the cookie jar AND parse the encoded token
# from the JSON body; the POST sends both.
csrf_json="$(curl -s "${AUTH[@]}" -c "${COOKIE_JAR}" "${CSRF_URL}")"
token="$(printf '%s' "${csrf_json}" | sed -E 's/.*"token":"([^"]*)".*/\1/')"

if [ -z "${token}" ] || [ "${token}" = "${csrf_json}" ]; then
	echo "  WARN: could not parse token from /csrf response; skipping." >&2
	echo "  /csrf returned: ${csrf_json}" >&2
else
	body="$(curl -s "${AUTH[@]}" -b "${COOKIE_JAR}" \
		-X POST -H 'Content-Type: text/plain' \
		-H "X-XSRF-TOKEN: ${token}" --data 'hello' "${ECHO_URL}")"
	if [ "${body}" = "echo: hello" ]; then
		echo "  PASS: token-bearing POST succeeded ('${body}')"
	else
		echo "  FAIL: expected 'echo: hello', got '${body}'" >&2
		pass=1
	fi
fi

exit "${pass}"
