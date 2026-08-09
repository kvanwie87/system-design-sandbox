#!/bin/bash
# ============================================================
# Send Sample Script
# Uploads sample CSV to LocalStack S3 input bucket, waits for
# Lambda processing, and displays the JSON output.
# ============================================================

set -e

ENDPOINT="http://localhost:4566"
INPUT_BUCKET="csv-input-bucket"
OUTPUT_BUCKET="csv-output-bucket"
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

# Wait for Lambda to process
echo ""
echo "--- Waiting for Lambda to process (10 seconds) ---"
sleep 10

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
    echo "Try waiting longer or check Lambda logs:"
    echo "  $AWS_CMD logs filter-log-events --log-group-name /aws/lambda/csv-processor"
fi
