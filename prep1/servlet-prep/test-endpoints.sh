#!/usr/bin/env bash
#
# Endpoint smoke test for the servlet-prep app.
# Exercises every code path in HelloServlet and JsonEchoServlet and asserts
# the expected HTTP status code for each.
#
# Usage:
#   ./test-endpoints.sh                 # tests http://localhost:8080/servlet-prep
#   BASE_URL=http://host:port/ctx ./test-endpoints.sh
#
# Requires: curl. Exits non-zero if any assertion fails.

set -u

BASE_URL="${BASE_URL:-http://localhost:8080/servlet-prep}"

pass=0
fail=0

# check <description> <expected-status> <curl-args...>
check() {
    local desc="$1"; shift
    local expected="$1"; shift

    # Capture body + status code in one request. Body goes to a temp file,
    # the status code is printed to stdout by curl's -w.
    local body_file
    body_file="$(mktemp)"
    local actual
    actual="$(curl -s -o "$body_file" -w '%{http_code}' "$@")"
    local body
    body="$(cat "$body_file")"
    rm -f "$body_file"

    if [ "$actual" = "$expected" ]; then
        printf '  PASS  [%s] %s (status %s)\n' "$expected" "$desc" "$actual"
        pass=$((pass + 1))
    else
        printf '  FAIL  [expected %s, got %s] %s\n' "$expected" "$actual" "$desc"
        printf '        body: %s\n' "$body"
        fail=$((fail + 1))
    fi
}

echo "Testing against: $BASE_URL"
echo

echo "--- Landing page ---"
check "GET / (welcome file index.html)" 200 "$BASE_URL/"

echo
echo "--- HelloServlet (/hello) ---"
check "GET /hello (no param)"            200 "$BASE_URL/hello"
check "GET /hello?name=Priya"            200 "$BASE_URL/hello?name=Priya"
check "POST /hello"                      200 -X POST "$BASE_URL/hello"

echo
echo "--- JsonEchoServlet (/echo) ---"
check "POST /echo valid JSON"            200 \
    -X POST -H "Content-Type: application/json" \
    -d '{"message":"hello"}' "$BASE_URL/echo"

check "POST /echo wrong content-type"    415 \
    -X POST -H "Content-Type: text/plain" \
    -d 'plain text' "$BASE_URL/echo"

check "POST /echo empty body"            400 \
    -X POST -H "Content-Type: application/json" \
    "$BASE_URL/echo"

check "GET /echo (method not allowed)"   405 "$BASE_URL/echo"

echo
echo "======================================"
printf 'Results: %d passed, %d failed\n' "$pass" "$fail"
echo "======================================"

[ "$fail" -eq 0 ]
