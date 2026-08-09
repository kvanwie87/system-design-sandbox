#!/bin/bash
# ============================================================
# Send Sample Script (Step Functions version)
# Uploads sample CSV to LocalStack S3 input bucket, waits for
# the Step Functions pipeline to complete, and displays output.
# ============================================================

set -e

ENDPOINT="http://localhost:4566"
INPUT_BUCKET="csv-input-bucket"
OUTPUT_BUCKET="csv-output-bucket"
STATE_MACHINE_NAME="csv-pipeline"
REGION="us-east-1"

# Disable trailing checksums (AWS CLI v2 feature not supported by LocalStack v2)
export AWS_REQUEST_CHECKSUM_CALCULATION=WHEN_REQUIRED
export AWS_RESPONSE_CHECKSUM_VALIDATION=WHEN_REQUIRED

# Use standard AWS CLI with LocalStack endpoint
AWS_CMD="aws --endpoint-url=$ENDPOINT --region $REGION"

echo "============================================"
echo " Sending sample CSV to LocalStack"
echo "============================================"

# Upload test file
echo ""
echo "--- Uploading test CSV ---"
$AWS_CMD s3 cp sample-data/orders.csv s3://$INPUT_BUCKET/orders.csv
echo "Uploaded: orders.csv -> s3://$INPUT_BUCKET/orders.csv"

# Wait for Step Functions to complete
echo ""
echo "--- Waiting for Step Functions pipeline (up to 3 minutes, checking every 15s) ---"
for i in $(seq 1 12); do
    sleep 15
    if $AWS_CMD s3 ls s3://$OUTPUT_BUCKET/orders.json 2>/dev/null; then
        echo "Output detected after $((i * 15)) seconds!"
        break
    fi
    echo "  Check $i/12 — not ready yet..."
done

# Check Step Functions execution status
echo ""
echo "--- Step Functions execution status ---"
SM_ARN=$($AWS_CMD stepfunctions list-state-machines --query "stateMachines[?name=='$STATE_MACHINE_NAME'].stateMachineArn" --output text 2>/dev/null || echo "")

if [ -n "$SM_ARN" ] && [ "$SM_ARN" != "None" ]; then
    EXECUTIONS=$($AWS_CMD stepfunctions list-executions --state-machine-arn "$SM_ARN" --max-results 1 --query 'executions[0].[executionArn,status]' --output text 2>/dev/null || echo "")
    if [ -n "$EXECUTIONS" ]; then
        echo "  Latest execution: $EXECUTIONS"
    fi
fi

# Check output
echo ""
echo "--- Checking output ---"
if $AWS_CMD s3 ls s3://$OUTPUT_BUCKET/orders.json 2>/dev/null; then
    echo ""
    echo "SUCCESS! Output file found. Content:"
    echo "----------------------------------------"
    $AWS_CMD s3 cp s3://$OUTPUT_BUCKET/orders.json -
    echo ""
    echo "----------------------------------------"
else
    echo ""
    echo "Output file not found yet."
    echo "The Step Functions pipeline may still be running (Java Lambda cold starts can be slow)."
    echo ""
    echo "Try:"
    echo "  - Wait longer and re-run this script"
    echo "  - Check execution: $AWS_CMD stepfunctions list-executions --state-machine-arn $SM_ARN"
    echo "  - Check logs: $AWS_CMD logs filter-log-events --log-group-name /aws/lambda/csv-download"
fi
