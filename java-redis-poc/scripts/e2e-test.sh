#!/bin/bash
set -e

BASE_URL="${BASE_URL:-http://localhost:8080}"
PASS=0
FAIL=0

test_endpoint() {
  local description="$1"
  local method="$2"
  local url="$3"
  local expected_code="$4"
  local data="$5"

  if [ -n "$data" ]; then
    content_type="text/plain"
    case "$data" in
      \{*|\[*) content_type="application/json" ;;
    esac
    actual_code=$(curl -s -o /dev/null -w "%{http_code}" -X "$method" "$url" -H "Content-Type: $content_type" -d "$data")
  else
    actual_code=$(curl -s -o /dev/null -w "%{http_code}" -X "$method" "$url")
  fi

  if [ "$actual_code" = "$expected_code" ]; then
    echo "PASS: $description"
    PASS=$((PASS + 1))
  else
    echo "FAIL: $description (expected $expected_code, got $actual_code)"
    FAIL=$((FAIL + 1))
  fi
}

echo "=== Redis PoC E2E Tests ==="
echo ""

# Strings (Feature Flags)
test_endpoint "Flag SET" PUT "$BASE_URL/api/flags/dark-mode?value=true" 200
test_endpoint "Flag SET with TTL" PUT "$BASE_URL/api/flags/promo-banner?value=Summer+Sale&ttlSeconds=86400" 200
test_endpoint "Flag GET" GET "$BASE_URL/api/flags/dark-mode" 200
test_endpoint "Flag SETNX (new)" POST "$BASE_URL/api/flags/beta-feature?value=false" 200
test_endpoint "Flag SETNX (exists)" POST "$BASE_URL/api/flags/dark-mode?value=changed" 200
test_endpoint "Flag TOGGLE" POST "$BASE_URL/api/flags/dark-mode/toggle" 200
test_endpoint "Flag LIST" GET "$BASE_URL/api/flags" 200
test_endpoint "Flag DELETE" DELETE "$BASE_URL/api/flags/promo-banner" 200
test_endpoint "Flag GET 404" GET "$BASE_URL/api/flags/nonexistent" 404

# Hashes (User Profiles)
test_endpoint "Hash PUT profile" PUT "$BASE_URL/api/hashes/users/1001" 200 '{"name":"Alice","email":"alice@example.com","age":"30"}'
test_endpoint "Hash GET profile" GET "$BASE_URL/api/hashes/users/1001" 200
test_endpoint "Hash GET field" GET "$BASE_URL/api/hashes/users/1001/name" 200
test_endpoint "Hash PUT field" PUT "$BASE_URL/api/hashes/users/1001/email" 200 "newemail@example.com"
test_endpoint "Hash DELETE field" DELETE "$BASE_URL/api/hashes/users/1001/email" 200
test_endpoint "Hash DELETE profile" DELETE "$BASE_URL/api/hashes/users/1001" 200

# Lists (Task Queue)
test_endpoint "Queue ENQUEUE" POST "$BASE_URL/api/queues/emails" 200 '{"type":"welcome","to":"alice@example.com"}'
test_endpoint "Queue ENQUEUE 2" POST "$BASE_URL/api/queues/emails" 200 '{"type":"reset","to":"bob@example.com"}'
test_endpoint "Queue PEEK" GET "$BASE_URL/api/queues/emails" 200
test_endpoint "Queue LENGTH" GET "$BASE_URL/api/queues/emails/length" 200
test_endpoint "Queue DEQUEUE" DELETE "$BASE_URL/api/queues/emails/next" 200

# Sets (Tags/Interests)
test_endpoint "Tags ADD alice:java" POST "$BASE_URL/api/tags/alice" 200 "java"
test_endpoint "Tags ADD alice:redis" POST "$BASE_URL/api/tags/alice" 200 "redis"
test_endpoint "Tags ADD alice:spring" POST "$BASE_URL/api/tags/alice" 200 "spring"
test_endpoint "Tags ADD bob:redis" POST "$BASE_URL/api/tags/bob" 200 "redis"
test_endpoint "Tags ADD bob:python" POST "$BASE_URL/api/tags/bob" 200 "python"
test_endpoint "Tags ADD bob:docker" POST "$BASE_URL/api/tags/bob" 200 "docker"
test_endpoint "Tags GET alice" GET "$BASE_URL/api/tags/alice" 200
test_endpoint "Tags HAS alice:java" GET "$BASE_URL/api/tags/alice/has/java" 200
test_endpoint "Tags COMMON" GET "$BASE_URL/api/tags/alice/common/bob" 200
test_endpoint "Tags UNIQUE" GET "$BASE_URL/api/tags/alice/unique/bob" 200
test_endpoint "Tags UNION" GET "$BASE_URL/api/tags/alice/union/bob" 200
test_endpoint "Tags COUNT" GET "$BASE_URL/api/tags/alice/count" 200
test_endpoint "Tags REMOVE" DELETE "$BASE_URL/api/tags/alice/spring" 200

# Sorted Sets (Task Scheduler)
test_endpoint "Scheduler ADD (due now)" POST "$BASE_URL/api/scheduler/jobs?taskId=send-email&executeAt=1000000000" 200
test_endpoint "Scheduler DELAY" POST "$BASE_URL/api/scheduler/jobs/delay?taskId=cleanup&delaySeconds=3600" 200
test_endpoint "Scheduler GET all" GET "$BASE_URL/api/scheduler/jobs" 200
test_endpoint "Scheduler GET due" GET "$BASE_URL/api/scheduler/jobs/due" 200
test_endpoint "Scheduler POP next" DELETE "$BASE_URL/api/scheduler/jobs/next" 200
test_endpoint "Scheduler COUNT" GET "$BASE_URL/api/scheduler/jobs/count" 200
test_endpoint "Scheduler CANCEL" DELETE "$BASE_URL/api/scheduler/jobs/cleanup" 200

# Cache
test_endpoint "Cache GET (miss)" GET "$BASE_URL/api/cache/demo-key" 200
test_endpoint "Cache GET (hit)" GET "$BASE_URL/api/cache/demo-key" 200
test_endpoint "Cache EVICT" DELETE "$BASE_URL/api/cache/demo-key" 200

# Sessions (login/use/logout with sliding TTL)
SESSION_ID=$(curl -s -X POST "$BASE_URL/api/sessions" -H "Content-Type: application/json" -d '{"username":"alice","role":"admin"}' | grep -o '"sessionId":"[^"]*"' | cut -d'"' -f4)
if [ -n "$SESSION_ID" ]; then
  echo "PASS: Session CREATE (got $SESSION_ID)"
  PASS=$((PASS + 1))
else
  echo "FAIL: Session CREATE (no sessionId returned)"
  FAIL=$((FAIL + 1))
  SESSION_ID="fake-id"
fi
test_endpoint "Session GET" GET "$BASE_URL/api/sessions/$SESSION_ID" 200
test_endpoint "Session UPDATE" PUT "$BASE_URL/api/sessions/$SESSION_ID" 200 '{"theme":"dark"}'
test_endpoint "Session DESTROY" DELETE "$BASE_URL/api/sessions/$SESSION_ID" 200
test_endpoint "Session GET 404" GET "$BASE_URL/api/sessions/$SESSION_ID" 404

# Pub/Sub (Domain Events)
test_endpoint "Events PUBLISH" POST "$BASE_URL/api/events/order.created" 200 '{"orderId":"123","total":59.99,"customer":"alice"}'
sleep 1
test_endpoint "Events GET" GET "$BASE_URL/api/events/order.created" 200
test_endpoint "Events LIST types" GET "$BASE_URL/api/events" 200
test_endpoint "Events CLEAR" DELETE "$BASE_URL/api/events/order.created" 200

# Rate Limiting
test_endpoint "Rate Limit CHECK" GET "$BASE_URL/api/rate-limit/check/e2e-client" 200

# Leaderboard
test_endpoint "Leaderboard ADD" POST "$BASE_URL/api/leaderboard?player=alice&score=500" 200
test_endpoint "Leaderboard ADD 2" POST "$BASE_URL/api/leaderboard?player=bob&score=300" 200
test_endpoint "Leaderboard TOP" GET "$BASE_URL/api/leaderboard/top/10" 200
test_endpoint "Leaderboard RANK" GET "$BASE_URL/api/leaderboard/rank/alice" 200
test_endpoint "Leaderboard INCREMENT" POST "$BASE_URL/api/leaderboard/increment?player=bob&increment=250" 200

echo ""
echo "=== Results: $PASS passed, $FAIL failed ==="

if [ $FAIL -gt 0 ]; then
  exit 1
fi
