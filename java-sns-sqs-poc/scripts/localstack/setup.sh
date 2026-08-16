#!/bin/bash
set -euo pipefail

# =============================================================================
# LocalStack Infrastructure Setup
# Creates: S3 bucket, SNS topic, 2 SQS queues, Lambda function, S3 notification
#
# Usage: ./setup.sh [--profile <profile>]
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
TOPIC_NAME="csv-processed"
QUEUE_AUDIT="audit-queue"
QUEUE_NOTIFICATION="notification-queue"
FUNCTION_NAME="csv-processor"
PROJECT_ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
JAR_PATH="${PROJECT_ROOT}/build/libs/csv-processor-lambda-all.jar"

# On Windows (Git Bash/MSYS), convert to Windows path for aws CLI fileb:// support
if command -v cygpath > /dev/null 2>&1; then
    JAR_PATH_FOR_AWS="$(cygpath -w "${JAR_PATH}")"
else
    JAR_PATH_FOR_AWS="${JAR_PATH}"
fi

# Use aws CLI directly with endpoint override (avoids awslocal issues on Windows)
awsl() {
    aws --endpoint-url="${ENDPOINT}" --region "${REGION}" ${PROFILE_ARG} "$@"
}

echo "============================================"
echo " LocalStack Infrastructure Setup"
echo "============================================"

# --- Wait for LocalStack to be healthy ---
echo ""
echo "[1/8] Waiting for LocalStack to be healthy..."
MAX_RETRIES=30
RETRY=0
until curl -sf "${ENDPOINT}/_localstack/health" > /dev/null 2>&1; do
    RETRY=$((RETRY + 1))
    if [ $RETRY -ge $MAX_RETRIES ]; then
        echo "ERROR: LocalStack did not become healthy after ${MAX_RETRIES} attempts"
        exit 1
    fi
    echo "  Waiting... (attempt ${RETRY}/${MAX_RETRIES})"
    sleep 2
done
echo "  LocalStack is healthy!"

# --- Create S3 bucket ---
echo ""
echo "[2/8] Creating S3 bucket: ${BUCKET_NAME}"
awsl s3 mb "s3://${BUCKET_NAME}" 2>/dev/null || echo "  Bucket already exists"

# --- Create SNS topic ---
echo ""
echo "[3/8] Creating SNS topic: ${TOPIC_NAME}"
TOPIC_ARN=$(awsl sns create-topic --name "${TOPIC_NAME}" --query 'TopicArn' --output text)
echo "  Topic ARN: ${TOPIC_ARN}"

# --- Create SQS queues ---
echo ""
echo "[4/8] Creating SQS queues: ${QUEUE_AUDIT}, ${QUEUE_NOTIFICATION}"
AUDIT_QUEUE_URL=$(awsl sqs create-queue --queue-name "${QUEUE_AUDIT}" --query 'QueueUrl' --output text)
NOTIFICATION_QUEUE_URL=$(awsl sqs create-queue --queue-name "${QUEUE_NOTIFICATION}" --query 'QueueUrl' --output text)
echo "  Audit queue URL: ${AUDIT_QUEUE_URL}"
echo "  Notification queue URL: ${NOTIFICATION_QUEUE_URL}"

AUDIT_QUEUE_ARN="arn:aws:sqs:${REGION}:${ACCOUNT_ID}:${QUEUE_AUDIT}"
NOTIFICATION_QUEUE_ARN="arn:aws:sqs:${REGION}:${ACCOUNT_ID}:${QUEUE_NOTIFICATION}"

# --- Subscribe queues to SNS topic ---
echo ""
echo "[5/8] Subscribing queues to SNS topic"
awsl sns subscribe \
    --topic-arn "${TOPIC_ARN}" \
    --protocol sqs \
    --notification-endpoint "${AUDIT_QUEUE_ARN}" \
    --output text > /dev/null
echo "  Subscribed: ${QUEUE_AUDIT}"

awsl sns subscribe \
    --topic-arn "${TOPIC_ARN}" \
    --protocol sqs \
    --notification-endpoint "${NOTIFICATION_QUEUE_ARN}" \
    --output text > /dev/null
echo "  Subscribed: ${QUEUE_NOTIFICATION}"

# --- Create IAM role for Lambda ---
echo ""
echo "[6/8] Creating IAM role for Lambda"
ROLE_NAME="lambda-csv-processor-role"
ASSUME_ROLE_POLICY='{
  "Version": "2012-10-17",
  "Statement": [{
    "Effect": "Allow",
    "Principal": {"Service": "lambda.amazonaws.com"},
    "Action": "sts:AssumeRole"
  }]
}'

awsl iam create-role \
    --role-name "${ROLE_NAME}" \
    --assume-role-policy-document "${ASSUME_ROLE_POLICY}" \
    --output text > /dev/null 2>&1 || echo "  Role already exists"

ROLE_ARN="arn:aws:iam::${ACCOUNT_ID}:role/${ROLE_NAME}"
echo "  Role ARN: ${ROLE_ARN}"

# --- Deploy Lambda function ---
echo ""
echo "[7/8] Deploying Lambda function: ${FUNCTION_NAME}"
if [ ! -f "${JAR_PATH}" ]; then
    echo "ERROR: Shadow JAR not found at ${JAR_PATH}"
    echo "Run './gradlew shadowJar' first."
    exit 1
fi

# Delete existing function if present (for idempotency)
awsl lambda delete-function --function-name "${FUNCTION_NAME}" 2>/dev/null || true

awsl lambda create-function \
    --function-name "${FUNCTION_NAME}" \
    --runtime java17 \
    --handler "com.example.processor.CsvProcessorHandler" \
    --role "${ROLE_ARN}" \
    --zip-file "fileb://${JAR_PATH_FOR_AWS}" \
    --timeout 60 \
    --memory-size 512 \
    --environment "Variables={AWS_ENDPOINT_URL=http://host.docker.internal:4566,SNS_TOPIC_ARN=${TOPIC_ARN}}" \
    --output text > /dev/null

echo "  Lambda deployed successfully"

# Wait for Lambda to be available
echo "  Waiting for Lambda to become available..."
sleep 3

# --- Configure S3 bucket notification ---
echo ""
echo "[8/8] Configuring S3 bucket notification to trigger Lambda"
LAMBDA_ARN="arn:aws:lambda:${REGION}:${ACCOUNT_ID}:function:${FUNCTION_NAME}"

NOTIFICATION_CONFIG="{
  \"LambdaFunctionConfigurations\": [{
    \"LambdaFunctionArn\": \"${LAMBDA_ARN}\",
    \"Events\": [\"s3:ObjectCreated:*\"]
  }]
}"

awsl s3api put-bucket-notification-configuration \
    --bucket "${BUCKET_NAME}" \
    --notification-configuration "${NOTIFICATION_CONFIG}"

echo "  S3 notification configured"

# --- Done ---
echo ""
echo "============================================"
echo " Setup Complete!"
echo "============================================"
echo ""
echo "Resources created:"
echo "  S3 Bucket:    s3://${BUCKET_NAME}"
echo "  SNS Topic:    ${TOPIC_ARN}"
echo "  SQS Queues:   ${AUDIT_QUEUE_URL}"
echo "                ${NOTIFICATION_QUEUE_URL}"
echo "  Lambda:       ${FUNCTION_NAME}"
echo ""
echo "Test with:"
echo "  ./scripts/localstack/test-e2e.sh"
