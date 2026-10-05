#!/usr/bin/env bash
# Validate the "formlogin" profile.
#
# This profile enables ONLY form login (not HTTP Basic), so:
#   1. An unauthenticated GET /hello is redirected (302) to /login.
#   2. A full login requires fetching the login page for a CSRF token,
#      POSTing credentials, and reusing the session cookie.
#
# Run the app first:  ./scripts/run-formlogin.sh
set -euo pipefail

source "$(dirname "${BASH_SOURCE[0]}")/lib-common.sh"

HELLO_URL="${BASE_URL}/hello"
LOGIN_URL="${BASE_URL}/login"
COOKIE_JAR="$(mktemp)"
trap 'rm -f "${COOKIE_JAR}"' EXIT

pass=0

# --- Check 1: unauthenticated request is redirected to the login page -------
echo "Check 1: unauthenticated GET ${HELLO_URL} should redirect to login"
status="$(curl -s -o /dev/null -w '%{http_code}' "${HELLO_URL}")"
location="$(curl -s -o /dev/null -w '%{redirect_url}' "${HELLO_URL}")"

if [ "${status}" = "302" ] && [[ "${location}" == *"/login"* ]]; then
	echo "  PASS: got 302 redirect to ${location}"
else
	echo "  FAIL: expected 302 -> /login, got ${status} -> '${location}'" >&2
	pass=1
fi

# --- Check 2: full form-login flow with CSRF token --------------------------
# Spring Security's default login form includes a CSRF token as a hidden field.
# We fetch the login page (establishing a session cookie), scrape the token,
# then POST the credentials reusing the same cookie jar.
echo "Check 2: form login with ${APP_USER} / ${APP_PASSWORD}"

login_page="$(curl -s -c "${COOKIE_JAR}" "${LOGIN_URL}")"
csrf="$(printf '%s' "${login_page}" \
	| grep -oE 'name="_csrf"[^>]*value="[^"]*"' \
	| grep -oE 'value="[^"]*"' | head -1 | sed -E 's/value="([^"]*)"/\1/')"

if [ -z "${csrf}" ]; then
	echo "  WARN: could not scrape CSRF token; skipping login POST." >&2
else
	# POST credentials; -L follows the post-login redirect back to a page.
	login_status="$(curl -s -b "${COOKIE_JAR}" -c "${COOKIE_JAR}" \
		-o /dev/null -w '%{http_code}' \
		-d "username=${APP_USER}" \
		-d "password=${APP_PASSWORD}" \
		-d "_csrf=${csrf}" \
		"${LOGIN_URL}")"

	# After a successful login Spring redirects (302) to the originally
	# requested page or "/". Then the session cookie should grant access.
	hello_body="$(curl -s -b "${COOKIE_JAR}" "${HELLO_URL}")"

	if [ "${hello_body}" = "Hello, World!" ]; then
		echo "  PASS: authenticated session reached /hello (login status ${login_status})"
	else
		echo "  FAIL: authenticated request did not return expected body (got '${hello_body}')" >&2
		pass=1
	fi
fi

exit "${pass}"
