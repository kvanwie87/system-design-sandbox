#!/bin/bash
# ============================================================
# LocalStack Setup Script
# Sets up S3 buckets, deploys the Lambda function, configures
# S3 event notification trigger, and runs a test upload.
# ============================================================

set -e

ENDPOINT="http://localhost:4566"
INPUT_BUCKET="csv-input-bucket"
OUTPUT_BUCKET="csv-output-bucket"
FUNCTION_NAME="csv-processor"
JAR_PATH="build/libs/java-lambda-poc-all.jar"
HANDLER="com.example.lambda.S3CsvProcessorHandler"
REGION="us-east-1"

# Disable trailing checksums (AWS CLI v2 feature not supported by LocalStack v2)
export AWS_REQUEST_CHECKSUM_CALCULATION=WHEN_REQUIRED
export AWS_RESPONSE_CHECKSUM_VALIDATION=WHEN_REQUIRED

# Use standard AWS CLI with LocalStack endpoint
AWS_CMD="aws --endpoint-url=$ENDPOINT --region $REGION"

echo "============================================"
echo " LocalStack Lambda POC Setup"
echo "============================================"
echo ""
echo "Using: aws --endpoint-url=$ENDPOINT --region $REGION"

# Wait for LocalStack to be ready
echo ""
echo "--- Waiting for LocalStack to be ready ---"
for i in {1..30}; do
    if curl -s "$ENDPOINT/_localstack/health" | grep -q '"s3": "available"'; then
        echo "LocalStack is ready!"
        break
    fi
    if [ $i -eq 30 ]; then
        echo "ERROR: LocalStack did not become ready in time."
        exit 1
    fi
    sleep 2
done

# Create S3 buckets
echo ""
echo "--- Creating S3 buckets ---"
$AWS_CMD s3 mb s3://$INPUT_BUCKET 2>/dev/null || echo "Input bucket already exists"
$AWS_CMD s3 mb s3://$OUTPUT_BUCKET 2>/dev/null || echo "Output bucket already exists"
echo "Created: $INPUT_BUCKET, $OUTPUT_BUCKET"

# Check if JAR exists
if [ ! -f "$JAR_PATH" ]; then
    echo ""
    echo "ERROR: Shadow JAR not found at $JAR_PATH"
    echo "Run './gradlew shadowJar' first."
    exit 1
fi

# Deploy Lambda function
echo ""
echo "--- Deploying Lambda function ---"
$AWS_CMD lambda delete-function --function-name $FUNCTION_NAME 2>/dev/null || true

$AWS_CMD lambda create-function \
    --function-name $FUNCTION_NAME \
    --runtime java17 \
    --handler $HANDLER \
    --role arn:aws:iam::000000000000:role/lambda-role \
    --zip-file fileb://$JAR_PATH \
    --timeout 60 \
    --memory-size 512 \
    --environment "Variables={OUTPUT_BUCKET=$OUTPUT_BUCKET,AWS_REGION=$REGION}"

echo "Lambda function '$FUNCTION_NAME' deployed."

# Wait for function to be active
echo ""
echo "--- Waiting for Lambda to be active ---"
sleep 3

# Add permission for S3 to invoke the Lambda
echo ""
echo "--- Adding S3 invoke permission to Lambda ---"
$AWS_CMD lambda add-permission \
    --function-name $FUNCTION_NAME \
    --statement-id s3-trigger \
    --action lambda:InvokeFunction \
    --principal s3.amazonaws.com \
    --source-arn arn:aws:s3:::$INPUT_BUCKET

# Configure S3 event notification
echo ""
echo "--- Configuring S3 trigger ---"
LAMBDA_ARN=$($AWS_CMD lambda get-function --function-name $FUNCTION_NAME --query 'Configuration.FunctionArn' --output text)

NOTIFICATION_CONFIG='{
  "LambdaFunctionConfigurations": [
    {
      "LambdaFunctionArn": "'"$LAMBDA_ARN"'",
      "Events": ["s3:ObjectCreated:*"],
      "Filter": {
        "Key": {
          "FilterRules": [
            {
              "Name": "suffix",
              "Value": ".csv"
            }
          ]
        }
      }
    }
  ]
}'

$AWS_CMD s3api put-bucket-notification-configuration \
    --bucket $INPUT_BUCKET \
    --notification-configuration "$NOTIFICATION_CONFIG"

echo "S3 trigger configured: $INPUT_BUCKET -> $FUNCTION_NAME (*.csv files)"

echo ""
echo "============================================"
echo " Setup complete!"
echo "============================================"
echo ""
echo "To test, run: ./scripts/send-sample.sh"
echo ""
echo "Useful commands:"
echo "  Upload a file:   $AWS_CMD s3 cp <file.csv> s3://$INPUT_BUCKET/"
echo "  Check output:    $AWS_CMD s3 ls s3://$OUTPUT_BUCKET/"
echo "  View logs:       $AWS_CMD logs filter-log-events --log-group-name /aws/lambda/$FUNCTION_NAME"
echo "  Invoke directly: $AWS_CMD lambda invoke --function-name $FUNCTION_NAME --payload '<event-json>' output.json"
