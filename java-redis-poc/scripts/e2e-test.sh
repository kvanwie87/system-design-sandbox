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
    actual_code=$(curl -s -o /dev/null -w "%{http_code}" -X "$method" "$url" -H "Content-Type: text/plain" -d "$data")
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

# Strings
test_endpoint "String PUT" PUT "$BASE_URL/api/strings/test-key" 200 "test-value"
test_endpoint "String GET" GET "$BASE_URL/api/strings/test-key" 200
test_endpoint "String DELETE" DELETE "$BASE_URL/api/strings/test-key" 200
test_endpoint "String GET 404" GET "$BASE_URL/api/strings/nonexistent" 404

# Hashes
test_endpoint "Hash PUT field" PUT "$BASE_URL/api/hashes/h1/name" 200 "Alice"
test_endpoint "Hash GET all" GET "$BASE_URL/api/hashes/h1" 200
test_endpoint "Hash GET field" GET "$BASE_URL/api/hashes/h1/name" 200
test_endpoint "Hash DELETE field" DELETE "$BASE_URL/api/hashes/h1/name" 200

# Lists
test_endpoint "List PUSH left" POST "$BASE_URL/api/lists/mylist?direction=left" 200 "item1"
test_endpoint "List PUSH right" POST "$BASE_URL/api/lists/mylist?direction=right" 200 "item2"
test_endpoint "List GET all" GET "$BASE_URL/api/lists/mylist" 200
test_endpoint "List POP left" DELETE "$BASE_URL/api/lists/mylist?direction=left" 200

# Sets
test_endpoint "Set ADD" POST "$BASE_URL/api/sets/myset" 200 "member1"
test_endpoint "Set GET all" GET "$BASE_URL/api/sets/myset" 200
test_endpoint "Set ISMEMBER" GET "$BASE_URL/api/sets/myset/member/member1" 200
test_endpoint "Set REMOVE" DELETE "$BASE_URL/api/sets/myset/member1" 200

# Sorted Sets
test_endpoint "ZSet ADD" POST "$BASE_URL/api/sorted-sets/zset1?member=player1&score=100" 200
test_endpoint "ZSet GET all" GET "$BASE_URL/api/sorted-sets/zset1" 200
test_endpoint "ZSet GET score" GET "$BASE_URL/api/sorted-sets/zset1/score/player1" 200
test_endpoint "ZSet GET range" GET "$BASE_URL/api/sorted-sets/zset1/range?start=0&end=-1" 200
test_endpoint "ZSet DELETE" DELETE "$BASE_URL/api/sorted-sets/zset1/player1" 200

# Cache
test_endpoint "Cache GET (miss)" GET "$BASE_URL/api/cache/demo-key" 200
test_endpoint "Cache GET (hit)" GET "$BASE_URL/api/cache/demo-key" 200
test_endpoint "Cache EVICT" DELETE "$BASE_URL/api/cache/demo-key" 200

# Sessions
test_endpoint "Session PUT" PUT "$BASE_URL/api/sessions/sess1?attributeKey=user" 200 "john"
test_endpoint "Session GET" GET "$BASE_URL/api/sessions/sess1" 200
test_endpoint "Session DELETE" DELETE "$BASE_URL/api/sessions/sess1" 200
test_endpoint "Session GET 404" GET "$BASE_URL/api/sessions/sess1" 404

# Pub/Sub
test_endpoint "PubSub PUBLISH" POST "$BASE_URL/api/pubsub/news" 200 "hello world"
test_endpoint "PubSub GET messages" GET "$BASE_URL/api/pubsub/news" 200
test_endpoint "PubSub CLEAR" DELETE "$BASE_URL/api/pubsub/news" 200

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
