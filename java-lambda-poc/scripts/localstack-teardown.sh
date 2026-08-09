#!/bin/bash
# ============================================================
# LocalStack Teardown Script (Step Functions version)
# Removes all resources created by localstack-setup.sh
# ============================================================

set -e

ENDPOINT="http://localhost:4566"
INPUT_BUCKET="csv-input-bucket"
OUTPUT_BUCKET="csv-output-bucket"
REGION="us-east-1"
STATE_MACHINE_NAME="csv-pipeline"

# Disable trailing checksums (AWS CLI v2 feature not supported by LocalStack v2)
export AWS_REQUEST_CHECKSUM_CALCULATION=WHEN_REQUIRED
export AWS_RESPONSE_CHECKSUM_VALIDATION=WHEN_REQUIRED

# Use standard AWS CLI with LocalStack endpoint
AWS_CMD="aws --endpoint-url=$ENDPOINT --region $REGION"

echo "============================================"
echo " LocalStack Teardown"
echo "============================================"

echo "--- Removing state machine ---"
SM_ARN=$($AWS_CMD stepfunctions list-state-machines --query "stateMachines[?name=='$STATE_MACHINE_NAME'].stateMachineArn" --output text 2>/dev/null || echo "")
if [ -n "$SM_ARN" ] && [ "$SM_ARN" != "None" ]; then
    $AWS_CMD stepfunctions delete-state-machine --state-machine-arn "$SM_ARN" 2>/dev/null || true
    echo "  Deleted: $STATE_MACHINE_NAME"
else
    echo "  State machine not found"
fi

echo "--- Removing Lambda functions ---"
for FUNC in csv-trigger csv-download csv-filter csv-enrich csv-output; do
    $AWS_CMD lambda delete-function --function-name $FUNC 2>/dev/null && echo "  Deleted: $FUNC" || echo "  Not found: $FUNC"
done

echo "--- Emptying and removing S3 buckets ---"
$AWS_CMD s3 rm s3://$INPUT_BUCKET --recursive 2>/dev/null || true
$AWS_CMD s3 rb s3://$INPUT_BUCKET 2>/dev/null || echo "Input bucket not found"
$AWS_CMD s3 rm s3://$OUTPUT_BUCKET --recursive 2>/dev/null || true
$AWS_CMD s3 rb s3://$OUTPUT_BUCKET 2>/dev/null || echo "Output bucket not found"

echo ""
echo "Teardown complete."
echo "To stop LocalStack: docker-compose down"
