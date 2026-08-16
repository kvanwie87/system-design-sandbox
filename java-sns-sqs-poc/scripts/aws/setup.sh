#!/bin/bash
set -euo pipefail

# =============================================================================
# AWS Infrastructure Setup
# Creates: S3 bucket, SNS topic, 2 SQS queues, IAM role, Lambda, S3 notification
# Requires: AWS CLI configured with appropriate credentials
#
# Usage: ./setup.sh [--profile <profile>]
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
PROJECT_ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
JAR_PATH="${PROJECT_ROOT}/build/libs/csv-processor-lambda-all.jar"

# On Windows (Git Bash/MSYS), convert to Windows path for aws CLI fileb:// support
if command -v cygpath > /dev/null 2>&1; then
    JAR_PATH_FOR_AWS="$(cygpath -w "${JAR_PATH}")"
else
    JAR_PATH_FOR_AWS="${JAR_PATH}"
fi

# Wrapper to include profile argument in all aws calls
awscli() {
    aws ${PROFILE_ARG} "$@"
}

# Get the AWS account ID
ACCOUNT_ID=$(awscli sts get-caller-identity --query 'Account' --output text)
BUCKET_NAME="csv-input-bucket-${ACCOUNT_ID}"

echo "============================================"
echo " AWS Infrastructure Setup"
echo "============================================"
echo " Region:  ${REGION}"
echo " Account: ${ACCOUNT_ID}"
echo "============================================"

# --- Create S3 bucket ---
echo ""
echo "[1/8] Creating S3 bucket: ${BUCKET_NAME} (region: ${REGION})"
EXISTING_BUCKET_REGION=$(awscli s3api get-bucket-location --bucket "${BUCKET_NAME}" --query 'LocationConstraint' --output text 2>/dev/null)
if [ $? -eq 0 ]; then
    # get-bucket-location returns "None" for us-east-1
    if [ "${EXISTING_BUCKET_REGION}" = "None" ] || [ "${EXISTING_BUCKET_REGION}" = "null" ]; then
        EXISTING_BUCKET_REGION="us-east-1"
    fi
    if [ "${EXISTING_BUCKET_REGION}" != "${REGION}" ]; then
        echo "  ERROR: Bucket already exists in region ${EXISTING_BUCKET_REGION}, but script targets ${REGION}."
        echo "  Delete it first: aws s3 rb s3://${BUCKET_NAME} --force --region ${EXISTING_BUCKET_REGION}"
        exit 1
    fi
    echo "  Bucket already exists in ${REGION}"
else
    if [ "${REGION}" = "us-east-1" ]; then
        awscli s3api create-bucket --bucket "${BUCKET_NAME}" --region "${REGION}"
    else
        awscli s3api create-bucket --bucket "${BUCKET_NAME}" --region "${REGION}" \
            --create-bucket-configuration LocationConstraint="${REGION}"
    fi
    echo "  Bucket created"
fi

# --- Create SNS topic ---
echo ""
echo "[2/8] Creating SNS topic: ${TOPIC_NAME}"
TOPIC_ARN=$(awscli sns create-topic --name "${TOPIC_NAME}" --region "${REGION}" --query 'TopicArn' --output text)
echo "  Topic ARN: ${TOPIC_ARN}"

# --- Create SQS queues ---
echo ""
echo "[3/8] Creating SQS queues: ${QUEUE_AUDIT}, ${QUEUE_NOTIFICATION}"
AUDIT_QUEUE_URL=$(awscli sqs create-queue --queue-name "${QUEUE_AUDIT}" --region "${REGION}" --query 'QueueUrl' --output text)
NOTIFICATION_QUEUE_URL=$(awscli sqs create-queue --queue-name "${QUEUE_NOTIFICATION}" --region "${REGION}" --query 'QueueUrl' --output text)
echo "  Audit queue URL: ${AUDIT_QUEUE_URL}"
echo "  Notification queue URL: ${NOTIFICATION_QUEUE_URL}"

AUDIT_QUEUE_ARN="arn:aws:sqs:${REGION}:${ACCOUNT_ID}:${QUEUE_AUDIT}"
NOTIFICATION_QUEUE_ARN="arn:aws:sqs:${REGION}:${ACCOUNT_ID}:${QUEUE_NOTIFICATION}"

# --- Set SQS policies to allow SNS to send messages ---
echo ""
echo "[4/8] Setting SQS queue policies for SNS access"

SQS_POLICY_AUDIT="{
  \"Version\": \"2012-10-17\",
  \"Statement\": [{
    \"Effect\": \"Allow\",
    \"Principal\": {\"Service\": \"sns.amazonaws.com\"},
    \"Action\": \"sqs:SendMessage\",
    \"Resource\": \"${AUDIT_QUEUE_ARN}\",
    \"Condition\": {\"ArnEquals\": {\"aws:SourceArn\": \"${TOPIC_ARN}\"}}
  }]
}"

SQS_POLICY_NOTIFICATION="{
  \"Version\": \"2012-10-17\",
  \"Statement\": [{
    \"Effect\": \"Allow\",
    \"Principal\": {\"Service\": \"sns.amazonaws.com\"},
    \"Action\": \"sqs:SendMessage\",
    \"Resource\": \"${NOTIFICATION_QUEUE_ARN}\",
    \"Condition\": {\"ArnEquals\": {\"aws:SourceArn\": \"${TOPIC_ARN}\"}}
  }]
}"

awscli sqs set-queue-attributes \
    --queue-url "${AUDIT_QUEUE_URL}" \
    --attributes "Policy=${SQS_POLICY_AUDIT}" \
    --region "${REGION}"
echo "  Audit queue policy set"

awscli sqs set-queue-attributes \
    --queue-url "${NOTIFICATION_QUEUE_URL}" \
    --attributes "Policy=${SQS_POLICY_NOTIFICATION}" \
    --region "${REGION}"
echo "  Notification queue policy set"

# --- Subscribe queues to SNS topic ---
echo ""
echo "[5/8] Subscribing queues to SNS topic"
awscli sns subscribe \
    --topic-arn "${TOPIC_ARN}" \
    --protocol sqs \
    --notification-endpoint "${AUDIT_QUEUE_ARN}" \
    --region "${REGION}" \
    --output text > /dev/null
echo "  Subscribed: ${QUEUE_AUDIT}"

awscli sns subscribe \
    --topic-arn "${TOPIC_ARN}" \
    --protocol sqs \
    --notification-endpoint "${NOTIFICATION_QUEUE_ARN}" \
    --region "${REGION}" \
    --output text > /dev/null
echo "  Subscribed: ${QUEUE_NOTIFICATION}"

# --- Create IAM role for Lambda ---
echo ""
echo "[6/8] Creating IAM role for Lambda"

ASSUME_ROLE_POLICY='{
  "Version": "2012-10-17",
  "Statement": [{
    "Effect": "Allow",
    "Principal": {"Service": "lambda.amazonaws.com"},
    "Action": "sts:AssumeRole"
  }]
}'

awscli iam create-role \
    --role-name "${ROLE_NAME}" \
    --assume-role-policy-document "${ASSUME_ROLE_POLICY}" \
    --region "${REGION}" \
    --output text > /dev/null 2>&1 || echo "  Role already exists"

ROLE_ARN="arn:aws:iam::${ACCOUNT_ID}:role/${ROLE_NAME}"
echo "  Role ARN: ${ROLE_ARN}"

# Attach policies
LAMBDA_POLICY="{
  \"Version\": \"2012-10-17\",
  \"Statement\": [
    {
      \"Effect\": \"Allow\",
      \"Action\": [
        \"logs:CreateLogGroup\",
        \"logs:CreateLogStream\",
        \"logs:PutLogEvents\"
      ],
      \"Resource\": \"arn:aws:logs:${REGION}:${ACCOUNT_ID}:*\"
    },
    {
      \"Effect\": \"Allow\",
      \"Action\": [
        \"s3:GetObject\"
      ],
      \"Resource\": \"arn:aws:s3:::${BUCKET_NAME}/*\"
    },
    {
      \"Effect\": \"Allow\",
      \"Action\": [
        \"sns:Publish\"
      ],
      \"Resource\": \"${TOPIC_ARN}\"
    }
  ]
}"

awscli iam put-role-policy \
    --role-name "${ROLE_NAME}" \
    --policy-name "csv-processor-policy" \
    --policy-document "${LAMBDA_POLICY}" \
    --region "${REGION}"
echo "  Inline policy attached"

# Wait for IAM role to propagate
echo "  Waiting 10s for IAM role propagation..."
sleep 10

# --- Deploy Lambda function ---
echo ""
echo "[7/8] Deploying Lambda function: ${FUNCTION_NAME}"
if [ ! -f "${JAR_PATH}" ]; then
    echo "ERROR: Shadow JAR not found at ${JAR_PATH}"
    echo "Run './gradlew shadowJar' first."
    exit 1
fi

# Try to create; if exists, update the code
if awscli lambda get-function --function-name "${FUNCTION_NAME}" --region "${REGION}" > /dev/null 2>&1; then
    echo "  Function exists, updating code..."
    awscli lambda update-function-code \
        --function-name "${FUNCTION_NAME}" \
        --zip-file "fileb://${JAR_PATH_FOR_AWS}" \
        --region "${REGION}" \
        --output text > /dev/null
    
    echo "  Waiting for code update to complete..."
    awscli lambda wait function-updated-v2 --function-name "${FUNCTION_NAME}" --region "${REGION}" 2>/dev/null || sleep 10

    awscli lambda update-function-configuration \
        --function-name "${FUNCTION_NAME}" \
        --environment "Variables={SNS_TOPIC_ARN=${TOPIC_ARN}}" \
        --region "${REGION}" \
        --output text > /dev/null
else
    awscli lambda create-function \
        --function-name "${FUNCTION_NAME}" \
        --runtime java17 \
        --handler "com.example.processor.CsvProcessorHandler" \
        --role "${ROLE_ARN}" \
        --zip-file "fileb://${JAR_PATH_FOR_AWS}" \
        --timeout 60 \
        --memory-size 512 \
        --environment "Variables={SNS_TOPIC_ARN=${TOPIC_ARN}}" \
        --region "${REGION}" \
        --output text > /dev/null
fi
echo "  Lambda deployed successfully"

# Wait for Lambda to be active
echo "  Waiting for Lambda to become active..."
awscli lambda wait function-active-v2 --function-name "${FUNCTION_NAME}" --region "${REGION}" 2>/dev/null || sleep 5

# --- Configure S3 bucket notification ---
echo ""
echo "[8/8] Configuring S3 bucket notification to trigger Lambda"
LAMBDA_ARN="arn:aws:lambda:${REGION}:${ACCOUNT_ID}:function:${FUNCTION_NAME}"

# Grant S3 permission to invoke Lambda
awscli lambda add-permission \
    --function-name "${FUNCTION_NAME}" \
    --statement-id "s3-invoke-${FUNCTION_NAME}" \
    --action "lambda:InvokeFunction" \
    --principal s3.amazonaws.com \
    --source-arn "arn:aws:s3:::${BUCKET_NAME}" \
    --source-account "${ACCOUNT_ID}" \
    --region "${REGION}" \
    --output text > /dev/null 2>&1 || echo "  Permission already exists"

NOTIFICATION_CONFIG="{
  \"LambdaFunctionConfigurations\": [{
    \"LambdaFunctionArn\": \"${LAMBDA_ARN}\",
    \"Events\": [\"s3:ObjectCreated:*\"]
  }]
}"

awscli s3api put-bucket-notification-configuration \
    --bucket "${BUCKET_NAME}" \
    --notification-configuration "${NOTIFICATION_CONFIG}" \
    --region "${REGION}"
echo "  S3 notification configured"

# --- Done ---
echo ""
echo "============================================"
echo " AWS Setup Complete!"
echo "============================================"
echo ""
echo "Resources created:"
echo "  S3 Bucket:    s3://${BUCKET_NAME}"
echo "  SNS Topic:    ${TOPIC_ARN}"
echo "  SQS Queues:   ${AUDIT_QUEUE_URL}"
echo "                ${NOTIFICATION_QUEUE_URL}"
echo "  Lambda:       ${FUNCTION_NAME}"
echo "  IAM Role:     ${ROLE_ARN}"
echo ""
echo "Test with:"
echo "  aws s3 cp samples/test.csv s3://${BUCKET_NAME}/"
echo "  # Wait a few seconds, then:"
echo "  aws sqs receive-message --queue-url ${AUDIT_QUEUE_URL} --region ${REGION}"
echo "  aws sqs receive-message --queue-url ${NOTIFICATION_QUEUE_URL} --region ${REGION}"
