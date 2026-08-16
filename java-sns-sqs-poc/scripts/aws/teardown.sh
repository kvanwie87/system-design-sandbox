#!/bin/bash
set -euo pipefail

# =============================================================================
# AWS Infrastructure Teardown
# Removes all resources created by setup.sh
#
# Usage: ./teardown.sh [--profile <profile>]
#   Environment: AWS_REGION (default: us-east-1)
# =============================================================================

# Parse arguments
PROFILE_ARG=""
while [[ $# -gt 0 ]]; do
    case $1 in
        --profile) PROFILE_ARG="--profile $2"; shift 2 ;;
        *) echo "Unknown argument: $1"; exit 1 ;;
    esac
done

REGION="${AWS_REGION:-us-east-1}"
TOPIC_NAME="csv-processed"
QUEUE_AUDIT="audit-queue"
QUEUE_NOTIFICATION="notification-queue"
FUNCTION_NAME="csv-processor"
ROLE_NAME="lambda-csv-processor-role"

# Wrapper to include profile argument in all aws calls
awscli() {
    aws ${PROFILE_ARG} "$@"
}

ACCOUNT_ID=$(awscli sts get-caller-identity --query 'Account' --output text)
BUCKET_NAME="csv-input-bucket-${ACCOUNT_ID}"
TOPIC_ARN="arn:aws:sns:${REGION}:${ACCOUNT_ID}:${TOPIC_NAME}"

echo "============================================"
echo " AWS Infrastructure Teardown"
echo "============================================"
echo " Region:  ${REGION}"
echo " Account: ${ACCOUNT_ID}"
echo "============================================"
echo ""
echo "WARNING: This will delete ALL resources created by setup-aws.sh."
echo "Press Ctrl+C to cancel, or wait 5 seconds to proceed..."
sleep 5

# --- Remove S3 bucket notification ---
echo ""
echo "[1/7] Removing S3 bucket notification..."
awscli s3api put-bucket-notification-configuration \
    --bucket "${BUCKET_NAME}" \
    --notification-configuration '{}' \
    --region "${REGION}" 2>/dev/null || echo "  Skipped (bucket may not exist)"

# --- Delete Lambda function ---
echo ""
echo "[2/7] Deleting Lambda function: ${FUNCTION_NAME}"
awscli lambda delete-function \
    --function-name "${FUNCTION_NAME}" \
    --region "${REGION}" 2>/dev/null || echo "  Already deleted or not found"

# --- Remove SNS subscriptions ---
echo ""
echo "[3/7] Removing SNS subscriptions..."
SUBS=$(awscli sns list-subscriptions-by-topic --topic-arn "${TOPIC_ARN}" --region "${REGION}" --query 'Subscriptions[].SubscriptionArn' --output text 2>/dev/null || echo "")
for SUB_ARN in ${SUBS}; do
    if [ "${SUB_ARN}" != "PendingConfirmation" ]; then
        awscli sns unsubscribe --subscription-arn "${SUB_ARN}" --region "${REGION}" 2>/dev/null || true
        echo "  Unsubscribed: ${SUB_ARN}"
    fi
done

# --- Delete SNS topic ---
echo ""
echo "[4/7] Deleting SNS topic: ${TOPIC_NAME}"
awscli sns delete-topic --topic-arn "${TOPIC_ARN}" --region "${REGION}" 2>/dev/null || echo "  Already deleted or not found"

# --- Delete SQS queues ---
echo ""
echo "[5/7] Deleting SQS queues..."
AUDIT_QUEUE_URL=$(awscli sqs get-queue-url --queue-name "${QUEUE_AUDIT}" --region "${REGION}" --query 'QueueUrl' --output text 2>/dev/null || echo "")
if [ -n "${AUDIT_QUEUE_URL}" ]; then
    awscli sqs delete-queue --queue-url "${AUDIT_QUEUE_URL}" --region "${REGION}"
    echo "  Deleted: ${QUEUE_AUDIT}"
else
    echo "  ${QUEUE_AUDIT} not found"
fi

NOTIFICATION_QUEUE_URL=$(awscli sqs get-queue-url --queue-name "${QUEUE_NOTIFICATION}" --region "${REGION}" --query 'QueueUrl' --output text 2>/dev/null || echo "")
if [ -n "${NOTIFICATION_QUEUE_URL}" ]; then
    awscli sqs delete-queue --queue-url "${NOTIFICATION_QUEUE_URL}" --region "${REGION}"
    echo "  Deleted: ${QUEUE_NOTIFICATION}"
else
    echo "  ${QUEUE_NOTIFICATION} not found"
fi

# --- Delete IAM role ---
echo ""
echo "[6/7] Deleting IAM role: ${ROLE_NAME}"
awscli iam delete-role-policy \
    --role-name "${ROLE_NAME}" \
    --policy-name "csv-processor-policy" \
    --region "${REGION}" 2>/dev/null || echo "  No inline policy found"
awscli iam delete-role \
    --role-name "${ROLE_NAME}" \
    --region "${REGION}" 2>/dev/null || echo "  Role not found"
echo "  IAM role deleted"

# --- Delete S3 bucket ---
echo ""
echo "[7/7] Deleting S3 bucket: ${BUCKET_NAME}"
awscli s3 rb "s3://${BUCKET_NAME}" --force --region "${REGION}" 2>/dev/null || echo "  Bucket not found or not empty"

# --- Done ---
echo ""
echo "============================================"
echo " Teardown Complete!"
echo "============================================"
