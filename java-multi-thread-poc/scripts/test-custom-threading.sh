#!/bin/bash
# ============================================================
# Test script for :custom-threading module (port 8081)
# Sends sample payloads to all endpoints and displays responses.
# 
# Prerequisites: the app must be running on port 8081
#   ./gradlew :custom-threading:bootRun
# ============================================================

BASE_URL="http://localhost:8081"

echo "========================================"
echo " Testing :custom-threading (port 8081)"
echo "========================================"

# --- Fire-and-forget ---
echo ""
echo "--- 1. Fire-and-Forget: POST /notifications/send ---"
echo "Sending notification..."
curl -s -w "\nHTTP Status: %{http_code}\n" \
  -X POST "$BASE_URL/notifications/send" \
  -H "Content-Type: application/json" \
  -d '{"message": "Hello from custom-threading test!"}'
echo "(Check app logs — notification completes ~2s later on a my-async-N thread)"

# --- Async with Polling ---
echo ""
echo ""
echo "--- 2. Async with Polling: POST /reports/generate ---"
echo "Starting report generation..."
RESPONSE=$(curl -s -X POST "$BASE_URL/reports/generate")
echo "Response: $RESPONSE"

# Extract taskId from JSON response
TASK_ID=$(echo "$RESPONSE" | grep -o '"taskId":"[^"]*"' | cut -d'"' -f4)

if [ -z "$TASK_ID" ]; then
  echo "ERROR: Could not extract taskId from response"
  exit 1
fi

echo ""
echo "--- 2a. Poll: GET /reports/$TASK_ID (immediately — should be PENDING) ---"
curl -s -w "\nHTTP Status: %{http_code}\n" "$BASE_URL/reports/$TASK_ID"

echo ""
echo "Waiting 6 seconds for report to complete..."
sleep 6

echo ""
echo "--- 2b. Poll: GET /reports/$TASK_ID (after 6s — should be COMPLETE) ---"
curl -s -w "\nHTTP Status: %{http_code}\n" "$BASE_URL/reports/$TASK_ID"

# --- Unknown task ID ---
echo ""
echo ""
echo "--- 3. Edge case: GET /reports/unknown-id (should be 404) ---"
curl -s -w "\nHTTP Status: %{http_code}\n" "$BASE_URL/reports/does-not-exist"

echo ""
echo ""
echo "========================================"
echo " Done! Check app logs for async output."
echo "========================================"
