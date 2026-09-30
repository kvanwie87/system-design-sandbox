#!/usr/bin/env bash
#
# auth-demo.sh — boot the webhook server + client and exercise the JWT auth on
# the registration endpoint (POST /api/webhooks/register).
#
# What it does:
#   1. Builds both modules (skipping tests for speed).
#   2. Starts the server in a chosen auth mode (spring-security | hand-rolled | none).
#   3. Runs a suite of auth assertions against the registration endpoint using
#      HS256 tokens minted here with openssl (independent of the Java code).
#   4. Starts a client, confirms it auto-registers and receives stock events.
#   5. Tears everything down and prints a pass/fail summary.
#
# Usage:
#   ./auth-demo.sh                 # defaults to hand-rolled mode
#   ./auth-demo.sh spring-security
#   ./auth-demo.sh hand-rolled
#   ./auth-demo.sh none            # endpoint open; only the "valid/none" checks apply
#
# Requirements: bash, curl, openssl, and a JDK 21 (auto-detected below).

set -u

# ---------------------------------------------------------------------------
# Configuration (must match src/main/resources/application.yaml auth settings)
# ---------------------------------------------------------------------------
MODE="${1:-hand-rolled}"
ISSUER="webhook-auth"
AUDIENCE="webhook-server"
CLIENT_ID="webhook-client"
CLIENT_SECRET="webhook-client-secret-change-me"

SERVER_PORT=8080
CLIENT_PORT=8081
SERVER_URL="http://localhost:${SERVER_PORT}"
REGISTER_URL="${SERVER_URL}/api/webhooks/register"
TOKEN_URL="${SERVER_URL}/oauth/token"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SERVER_DIR="${SCRIPT_DIR}/java-webhook-poc-server"
CLIENT_DIR="${SCRIPT_DIR}/java-webhook-poc-client"

SERVER_LOG="$(mktemp -t webhook-server.XXXXXX.log)"
CLIENT_LOG="$(mktemp -t webhook-client.XXXXXX.log)"
SERVER_PID=""
CLIENT_PID=""

PASS=0
FAIL=0

# ---------------------------------------------------------------------------
# Pretty output helpers
# ---------------------------------------------------------------------------
if [ -t 1 ]; then
  GREEN=$'\033[32m'; RED=$'\033[31m'; BLUE=$'\033[34m'; DIM=$'\033[2m'; RESET=$'\033[0m'
else
  GREEN=""; RED=""; BLUE=""; DIM=""; RESET=""
fi

info()  { echo "${BLUE}==>${RESET} $*"; }
note()  { echo "    ${DIM}$*${RESET}"; }

# Assert an actual HTTP status equals the expected one.
check() {
  local label="$1" expected="$2" actual="$3"
  if [ "$actual" = "$expected" ]; then
    echo "    ${GREEN}PASS${RESET} ${label} (HTTP ${actual})"
    PASS=$((PASS + 1))
  else
    echo "    ${RED}FAIL${RESET} ${label} (expected HTTP ${expected}, got ${actual})"
    FAIL=$((FAIL + 1))
  fi
}

# Run one registration case with full request/response logging then assert.
# Args: <label> <expected-status> <auth-header-or-empty>
run_check() {
  local label="$1" expected="$2" auth="$3"
  echo
  echo "  ${BLUE}CASE:${RESET} ${label}"
  do_register "$label" "$auth"
  check "$label" "$expected" "$LAST_STATUS"
}

# ---------------------------------------------------------------------------
# JDK detection — prefer JAVA_HOME if valid, else look in common locations.
# ---------------------------------------------------------------------------
detect_java_home() {
  if [ -n "${JAVA_HOME:-}" ] && [ -x "${JAVA_HOME}/bin/java" ]; then
    return
  fi
  local candidates=(
    "/c/Program Files/Amazon Corretto/jdk21.0.4_7"
    "/c/Program Files/Eclipse Adoptium"/jdk-21*
    "/c/Program Files/Java"/jdk-21*
    "/usr/lib/jvm"/*21*
    "/Library/Java/JavaVirtualMachines"/*21*/Contents/Home
  )
  for c in "${candidates[@]}"; do
    if [ -x "${c}/bin/java" ]; then
      export JAVA_HOME="$c"
      return
    fi
  done
  echo "${RED}Could not find a JDK 21. Set JAVA_HOME and retry.${RESET}" >&2
  exit 1
}

# ---------------------------------------------------------------------------
# Local RS256 forging with openssl — used ONLY for the negative test cases
# (expired / wrong-audience / wrong-key). We sign these with a THROWAWAY RSA key
# that the server does not trust, so a valid-looking token is still rejected.
# The happy-path token is obtained from the server's token endpoint, which signs
# with the server's real private key.
# ---------------------------------------------------------------------------
FORGE_KEY=""  # path to a throwaway private key, created on first use

b64url() { openssl base64 -e -A | tr '+/' '-_' | tr -d '='; }

ensure_forge_key() {
  if [ -z "$FORGE_KEY" ]; then
    FORGE_KEY="$(mktemp -t forge-key.XXXXXX.pem)"
    openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out "$FORGE_KEY" 2>/dev/null
  fi
}

# mint <exp_epoch> [audience] -> prints a compact RS256 JWT signed with the
# throwaway key (NOT the server's key). Used for negative cases.
mint() {
  local exp="$1"
  local aud="${2:-$AUDIENCE}"
  local now; now="$(date +%s)"
  ensure_forge_key
  local header payload signing_input sig
  header="$(printf '%s' '{"alg":"RS256","typ":"JWT"}' | b64url)"
  payload="$(printf '%s' "{\"sub\":\"webhook-client\",\"iss\":\"${ISSUER}\",\"aud\":\"${aud}\",\"iat\":${now},\"exp\":${exp}}" | b64url)"
  signing_input="${header}.${payload}"
  sig="$(printf '%s' "$signing_input" | openssl dgst -sha256 -sign "$FORGE_KEY" -binary | b64url)"
  printf '%s.%s' "$signing_input" "$sig"
}

# Shorten a token for readable logging: keep head/tail, elide the middle.
preview_token() {
  local t="$1"
  local n=${#t}
  if [ "$n" -le 24 ]; then echo "$t"; else echo "${t:0:12}...${t: -8} (${n} chars)"; fi
}

# Request an access token from the server's client-credentials token endpoint.
# Args: <client_id> <client_secret>. Sets TOKEN_STATUS, TOKEN_BODY, TOKEN_VALUE.
fetch_token() {
  local cid="$1" csecret="$2"
  local resp_file; resp_file="$(mktemp)"
  TOKEN_STATUS="$(curl -s -o "$resp_file" -w '%{http_code}' -X POST "$TOKEN_URL" \
    -H 'Content-Type: application/x-www-form-urlencoded' \
    --data-urlencode 'grant_type=client_credentials' \
    --data-urlencode "client_id=${cid}" \
    --data-urlencode "client_secret=${csecret}")"
  TOKEN_BODY="$(cat "$resp_file")"
  rm -f "$resp_file"
  # Extract access_token from the JSON body (simple field grab).
  TOKEN_VALUE="$(printf '%s' "$TOKEN_BODY" | sed -n 's/.*"access_token":"\([^"]*\)".*/\1/p')"
}

# POST a registration request, logging the request and response.
# Args: <label> <auth-header-or-empty>. Sets globals LAST_STATUS and LAST_BODY.
do_register() {
  local label="$1" auth="$2"
  local body='{"callbackUrl":"http://localhost:9999/webhook/events"}'
  local resp_file; resp_file="$(mktemp)"

  note "REQUEST : POST ${REGISTER_URL}"
  if [ -n "$auth" ]; then
    local tok="${auth#Bearer }"
    note "          Authorization: Bearer $(preview_token "$tok")"
    LAST_STATUS="$(curl -s -o "$resp_file" -w '%{http_code}' -X POST "$REGISTER_URL" \
      -H 'Content-Type: application/json' -H "Authorization: ${auth}" -d "$body")"
  else
    note "          (no Authorization header)"
    LAST_STATUS="$(curl -s -o "$resp_file" -w '%{http_code}' -X POST "$REGISTER_URL" \
      -H 'Content-Type: application/json' -d "$body")"
  fi
  LAST_BODY="$(cat "$resp_file")"
  rm -f "$resp_file"
  note "RESPONSE: HTTP ${LAST_STATUS} ${LAST_BODY}"
}

# True if something is already listening on the given TCP port.
port_in_use() {
  local p="$1"
  if command -v curl >/dev/null 2>&1 && curl -s -o /dev/null "http://localhost:${p}" 2>/dev/null; then
    return 0
  fi
  # Fall back to netstat if available (Windows/Git-Bash and most Unixes).
  if command -v netstat >/dev/null 2>&1; then
    netstat -an 2>/dev/null | grep -qE "[:.]${p}[[:space:]].*LISTEN" && return 0
  fi
  return 1
}

# Pick the first free port at or after the given starting port.
find_free_port() {
  local p="$1"
  for _ in $(seq 1 20); do
    if ! port_in_use "$p"; then echo "$p"; return 0; fi
    p=$((p + 1))
  done
  echo "$1"  # give up and return the original; startup will report the conflict
}

wait_for_server() {
  info "Waiting for server on ${SERVER_URL} ..."
  for _ in $(seq 1 60); do
    # Any HTTP response (even 401) means the port is up.
    if curl -s -o /dev/null "$REGISTER_URL" 2>/dev/null; then
      note "server is accepting connections"
      return 0
    fi
    sleep 1
  done
  echo "${RED}Server did not come up in time. Recent log:${RESET}" >&2
  tail -n 30 "$SERVER_LOG" >&2
  return 1
}

# Kill whatever process is listening on the given TCP port. On Windows/Git-Bash
# the forked JVM is a child of the gradle wrapper, so killing the wrapper PID is
# not enough — we target the actual listener by port.
kill_listener_on_port() {
  local p="$1"
  if command -v netstat >/dev/null 2>&1 && command -v taskkill >/dev/null 2>&1; then
    # Windows: find the PID in the last column of the LISTENING row.
    local pid
    pid="$(netstat -ano 2>/dev/null | grep -E ":${p}[[:space:]].*LISTENING" | awk '{print $NF}' | head -1)"
    if [ -n "$pid" ]; then taskkill //PID "$pid" //F >/dev/null 2>&1; fi
  elif command -v lsof >/dev/null 2>&1; then
    # Unix: kill by port via lsof.
    local pid
    pid="$(lsof -ti tcp:"${p}" 2>/dev/null | head -1)"
    if [ -n "$pid" ]; then kill -9 "$pid" 2>/dev/null; fi
  fi
}

cleanup() {
  info "Shutting down ..."
  # First try to stop the gradle wrapper processes we launched.
  [ -n "$CLIENT_PID" ] && kill "$CLIENT_PID" 2>/dev/null
  [ -n "$SERVER_PID" ] && kill "$SERVER_PID" 2>/dev/null
  sleep 2
  [ -n "$CLIENT_PID" ] && kill -9 "$CLIENT_PID" 2>/dev/null
  [ -n "$SERVER_PID" ] && kill -9 "$SERVER_PID" 2>/dev/null
  # Then make sure the forked JVMs bound to our ports are gone too.
  kill_listener_on_port "$CLIENT_PORT"
  kill_listener_on_port "$SERVER_PORT"
  [ -n "$FORGE_KEY" ] && rm -f "$FORGE_KEY"
  note "server log: ${SERVER_LOG}"
  note "client log: ${CLIENT_LOG}"
}
trap cleanup EXIT INT TERM

# ---------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------
case "$MODE" in
  spring-security|hand-rolled|none) ;;
  *) echo "${RED}Invalid mode '${MODE}'. Use: spring-security | hand-rolled | none${RESET}" >&2; exit 1 ;;
esac

detect_java_home
info "Using JAVA_HOME=${JAVA_HOME}"
info "Auth mode: ${MODE}"

# Preflight: make sure the server port is free, and pick a free client port.
if port_in_use "$SERVER_PORT"; then
  echo "${RED}Port ${SERVER_PORT} is already in use. Stop the process using it and retry.${RESET}" >&2
  exit 1
fi
CLIENT_PORT="$(find_free_port "$CLIENT_PORT")"
note "client will use port ${CLIENT_PORT}"

info "Building both modules (tests skipped) ..."
if ! (cd "$SERVER_DIR" && ./gradlew -q build -x test) ; then
  echo "${RED}Server build failed${RESET}" >&2; exit 1
fi
if ! (cd "$CLIENT_DIR" && ./gradlew -q build -x test) ; then
  echo "${RED}Client build failed${RESET}" >&2; exit 1
fi

info "Starting server (mode=${MODE}) ..."
# Note: no -q here so the application's auth logging is captured in the log file.
( cd "$SERVER_DIR" && ./gradlew --console=plain bootRun \
    --args="--webhook.auth.mode=${MODE} --webhook.event.interval=3000" ) \
    > "$SERVER_LOG" 2>&1 &
SERVER_PID=$!

wait_for_server || exit 1

now="$(date +%s)"

# ---------------------------------------------------------------------------
# Token endpoint (client-credentials grant) — the server mints the token.
# ---------------------------------------------------------------------------
echo
info "Token endpoint checks (POST ${TOKEN_URL})"

echo
echo "  ${BLUE}CASE:${RESET} valid client credentials issue a token"
fetch_token "$CLIENT_ID" "$CLIENT_SECRET"
note "REQUEST : POST ${TOKEN_URL} (grant=client_credentials, client_id=${CLIENT_ID})"
note "RESPONSE: HTTP ${TOKEN_STATUS} $(printf '%s' "$TOKEN_BODY" | cut -c1-80)"
check "valid credentials -> token issued" 200 "$TOKEN_STATUS"
SERVER_TOKEN="$TOKEN_VALUE"
if [ -n "$SERVER_TOKEN" ]; then
  note "issued token: $(preview_token "$SERVER_TOKEN")"
fi

echo
echo "  ${BLUE}CASE:${RESET} bad client secret is rejected"
fetch_token "$CLIENT_ID" "wrong-secret"
note "RESPONSE: HTTP ${TOKEN_STATUS} ${TOKEN_BODY}"
check "bad client secret -> rejected" 401 "$TOKEN_STATUS"

# ---------------------------------------------------------------------------
# Registration endpoint — protected by the JWT (except in mode=none).
# The happy-path token is the one the SERVER just issued above.
# ---------------------------------------------------------------------------
echo
info "Registration endpoint auth checks (watch the server log for matching auth entries)"

if [ "$MODE" = "none" ]; then
  # No auth enforced: any request (with or without a token) should be accepted.
  run_check "no token is accepted (mode=none)"          200 ""
  run_check "server-issued token also accepted (none)"  200 "Bearer ${SERVER_TOKEN}"
else
  # The valid token comes from the server's token endpoint (signed with the
  # server's private key). The negatives are forged locally with a THROWAWAY
  # RSA key the server doesn't trust — demonstrating that a verifier holding only
  # the public key cannot be fooled by a token it didn't sign.
  wrong_key_valid_claims="$(mint $((now + 300)))"
  wrong_key_expired="$(mint $((now - 10)))"

  run_check "server-issued token accepted"          200 "Bearer ${SERVER_TOKEN}"
  run_check "missing token rejected"                 401 ""
  run_check "garbage token rejected"                 401 "Bearer not.a.jwt"
  run_check "token signed by wrong key rejected"     401 "Bearer ${wrong_key_valid_claims}"
  run_check "wrong-key + expired token rejected"     401 "Bearer ${wrong_key_expired}"
fi

# Surface the server-side auth log lines produced by the checks above, so the
# Java logging (ACCEPTED / REJECTED with reasons) is visible right here.
echo
info "Server-side auth log (recent entries)"
sleep 1  # give the server a moment to flush its log to disk
grep -aE "auth:|Auth mode:|token endpoint:" "$SERVER_LOG" | tail -n 24 | sed "s/^/    ${DIM}/; s/$/${RESET}/"

echo
info "Starting client on port ${CLIENT_PORT} (auto-registers, then receives events) ..."
# Note: no -q here so the application's auth logging is captured in the log file.
( cd "$CLIENT_DIR" && ./gradlew --console=plain bootRun \
    --args="--webhook.auth.mode=${MODE} --server.port=${CLIENT_PORT}" ) \
    > "$CLIENT_LOG" 2>&1 &
CLIENT_PID=$!

# Confirm the client successfully registered with the server.
registered=false
for _ in $(seq 1 60); do
  if grep -q "Successfully registered with webhook server" "$CLIENT_LOG" 2>/dev/null; then
    registered=true
    break
  fi
  sleep 1
done
if $registered; then
  echo "    ${GREEN}PASS${RESET} client auto-registered with the server"
  PASS=$((PASS + 1))
else
  echo "    ${RED}FAIL${RESET} client did not register (see ${CLIENT_LOG})"
  FAIL=$((FAIL + 1))
fi

# Confirm the client receives at least one broadcast stock event.
received=false
for _ in $(seq 1 20); do
  if grep -q "\[STOCK EVENT\]" "$CLIENT_LOG" 2>/dev/null; then
    received=true
    break
  fi
  sleep 1
done
if $received; then
  echo "    ${GREEN}PASS${RESET} client received a stock event"
  PASS=$((PASS + 1))
  note "$(grep "\[STOCK EVENT\]" "$CLIENT_LOG" | tail -n 1)"
else
  echo "    ${RED}FAIL${RESET} client did not receive an event (see ${CLIENT_LOG})"
  FAIL=$((FAIL + 1))
fi

# Surface the client-side auth log lines (token minting / attaching).
echo
info "Client-side auth log (recent entries)"
grep -aE "Requesting access token|Received access token|Attaching Bearer|WITHOUT an Authorization|without authentication|Auth mode" "$CLIENT_LOG" \
  | tail -n 10 | sed "s/^/    ${DIM}/; s/$/${RESET}/"

echo
info "Summary: ${GREEN}${PASS} passed${RESET}, ${RED}${FAIL} failed${RESET} (mode=${MODE})"
[ "$FAIL" -eq 0 ]
