#!/usr/bin/env bash
# Validate the "permitall" profile.
# Expectation: GET /hello returns 200 with no credentials.
#
# Run the app first:  ./scripts/run-permitall.sh
set -euo pipefail

source "$(dirname "${BASH_SOURCE[0]}")/lib-common.sh"

URL="${BASE_URL}/hello"
echo "Validating permitall: GET ${URL} (no auth)"

status="$(curl -s -o /tmp/permitall-body.txt -w '%{http_code}' "${URL}")"
body="$(cat /tmp/permitall-body.txt)"

if [ "${status}" = "200" ] && [ "${body}" = "Hello, World!" ]; then
	echo "PASS: got 200 and expected body without authentication."
	exit 0
else
	echo "FAIL: expected 200 'Hello, World!', got ${status} '${body}'." >&2
	exit 1
fi
