#!/bin/bash
# ============================================================
# LocalStack Teardown Script
# Removes all resources created by localstack-setup.sh
# ============================================================

set -e

ENDPOINT="http://localhost:4566"
INPUT_BUCKET="csv-input-bucket"
OUTPUT_BUCKET="csv-output-bucket"
FUNCTION_NAME="csv-processor"
REGION="us-east-1"

# Disable trailing checksums (AWS CLI v2 feature not supported by LocalStack v2)
export AWS_REQUEST_CHECKSUM_CALCULATION=WHEN_REQUIRED
export AWS_RESPONSE_CHECKSUM_VALIDATION=WHEN_REQUIRED

# Use standard AWS CLI with LocalStack endpoint
AWS_CMD="aws --endpoint-url=$ENDPOINT --region $REGION"

echo "============================================"
echo " LocalStack Teardown"
echo "============================================"

echo "--- Removing Lambda function ---"
$AWS_CMD lambda delete-function --function-name $FUNCTION_NAME 2>/dev/null || echo "Function not found"

echo "--- Emptying and removing S3 buckets ---"
$AWS_CMD s3 rm s3://$INPUT_BUCKET --recursive 2>/dev/null || true
$AWS_CMD s3 rb s3://$INPUT_BUCKET 2>/dev/null || echo "Input bucket not found"
$AWS_CMD s3 rm s3://$OUTPUT_BUCKET --recursive 2>/dev/null || true
$AWS_CMD s3 rb s3://$OUTPUT_BUCKET 2>/dev/null || echo "Output bucket not found"

echo ""
echo "Teardown complete."
echo "To stop LocalStack: docker-compose down"
