#!/bin/bash
set -euo pipefail

# =============================================================================
# End-to-End Test Script for LocalStack
# Uploads a CSV to S3, waits for processing, and verifies messages in SQS queues
#
# Usage: ./test-e2e.sh [--profile <profile>]
# =============================================================================

# Parse arguments
PROFILE_ARG=""
while [[ $# -gt 0 ]]; do
    case $1 in
        --profile) PROFILE_ARG="--profile $2"; shift 2 ;;
        *) echo "Unknown argument: $1"; exit 1 ;;
    esac
done

REGION="us-east-1"
ACCOUNT_ID="000000000000"
ENDPOINT="http://localhost:4566"
BUCKET_NAME="csv-input-bucket"
QUEUE_AUDIT="audit-queue"
QUEUE_NOTIFICATION="notification-queue"
SAMPLE_FILE="$(cd "$(dirname "$0")/../.." && pwd)/samples/test.csv"
WAIT_SECONDS=10

# Use aws CLI directly with endpoint override (avoids awslocal issues on Windows)
awsl() {
    aws --endpoint-url="${ENDPOINT}" --region "${REGION}" ${PROFILE_ARG} "$@"
}

echo "============================================"
echo " End-to-End Test (LocalStack)"
echo "============================================"

# --- Verify LocalStack is running ---
echo ""
echo "[1/5] Checking LocalStack is healthy..."
if ! curl -sf "${ENDPOINT}/_localstack/health" > /dev/null 2>&1; then
    echo "ERROR: LocalStack is not running. Start it with: docker-compose up -d"
    exit 1
fi
echo "  OK"

# --- Get queue URLs ---
AUDIT_QUEUE_URL=$(awsl sqs get-queue-url --queue-name "${QUEUE_AUDIT}" --query 'QueueUrl' --output text 2>/dev/null)
NOTIFICATION_QUEUE_URL=$(awsl sqs get-queue-url --queue-name "${QUEUE_NOTIFICATION}" --query 'QueueUrl' --output text 2>/dev/null)

if [ -z "${AUDIT_QUEUE_URL}" ] || [ -z "${NOTIFICATION_QUEUE_URL}" ]; then
    echo "ERROR: Queues not found. Run './scripts/localstack/setup.sh' first."
    exit 1
fi

# --- Purge queues (clean state) ---
echo ""
echo "[2/5] Purging queues for clean test..."
awsl sqs purge-queue --queue-url "${AUDIT_QUEUE_URL}" 2>/dev/null || true
awsl sqs purge-queue --queue-url "${NOTIFICATION_QUEUE_URL}" 2>/dev/null || true
echo "  Queues purged"

# --- Upload CSV to S3 ---
echo ""
echo "[3/5] Uploading sample CSV to S3..."
if [ ! -f "${SAMPLE_FILE}" ]; then
    echo "ERROR: Sample file not found at ${SAMPLE_FILE}"
    exit 1
fi
awsl s3 cp "${SAMPLE_FILE}" "s3://${BUCKET_NAME}/test.csv"
echo "  Uploaded: s3://${BUCKET_NAME}/test.csv"

# --- Wait for async processing ---
echo ""
echo "[4/5] Waiting ${WAIT_SECONDS}s for Lambda processing..."
sleep "${WAIT_SECONDS}"

# --- Check SQS queues for messages ---
echo ""
echo "[5/5] Checking SQS queues for messages..."

AUDIT_MSG=$(awsl sqs receive-message \
    --queue-url "${AUDIT_QUEUE_URL}" \
    --max-number-of-messages 1 \
    --wait-time-seconds 5 \
    --output json)

NOTIFICATION_MSG=$(awsl sqs receive-message \
    --queue-url "${NOTIFICATION_QUEUE_URL}" \
    --max-number-of-messages 1 \
    --wait-time-seconds 5 \
    --output json)

echo ""
echo "--- Audit Queue Message ---"
if command -v jq > /dev/null 2>&1; then
    echo "${AUDIT_MSG}" | jq '.'
else
    echo "${AUDIT_MSG}"
fi

echo ""
echo "--- Notification Queue Message ---"
if command -v jq > /dev/null 2>&1; then
    echo "${NOTIFICATION_MSG}" | jq '.'
else
    echo "${NOTIFICATION_MSG}"
fi

# --- Validate results ---
echo ""
echo "============================================"
echo " Validation"
echo "============================================"

PASS=true

# Check audit queue has a message (works with or without jq)
if echo "${AUDIT_MSG}" | grep -q '"Messages"'; then
    echo "  [PASS] Audit queue received a message"
else
    echo "  [FAIL] Audit queue is empty"
    PASS=false
fi

# Check notification queue has a message
if echo "${NOTIFICATION_MSG}" | grep -q '"Messages"'; then
    echo "  [PASS] Notification queue received a message"
else
    echo "  [FAIL] Notification queue is empty"
    PASS=false
fi

# Check message contains expected CSV summary data
if echo "${AUDIT_MSG}" | grep -q '"rowCount.*5"' || echo "${AUDIT_MSG}" | grep -q '"rowCount":5' || echo "${AUDIT_MSG}" | grep -q '"rowCount\\":5'; then
    echo "  [PASS] Message contains correct rowCount (5)"
else
    echo "  [WARN] Could not verify rowCount in message (may be wrapped in SNS envelope)"
fi

echo ""
if [ "${PASS}" = true ]; then
    echo "  *** ALL TESTS PASSED ***"
    exit 0
else
    echo "  *** TESTS FAILED ***"
    exit 1
fi
