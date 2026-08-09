#!/bin/bash
# ============================================================
# LocalStack Setup Script (Step Functions version)
# Sets up S3 buckets, deploys Lambda functions, creates the
# Step Functions state machine, and configures the S3 trigger.
# ============================================================

set -e

ENDPOINT="http://localhost:4566"
INPUT_BUCKET="csv-input-bucket"
OUTPUT_BUCKET="csv-output-bucket"
REGION="us-east-1"
JAR_PATH="build/libs/java-lambda-poc-all.jar"
STATE_MACHINE_NAME="csv-pipeline"

# Lambda function names
TRIGGER_FUNCTION="csv-trigger"
DOWNLOAD_FUNCTION="csv-download"
FILTER_FUNCTION="csv-filter"
ENRICH_FUNCTION="csv-enrich"
OUTPUT_FUNCTION="csv-output"

# Handlers
TRIGGER_HANDLER="com.example.lambda.StepFunctionTriggerHandler"
DOWNLOAD_HANDLER="com.example.lambda.steps.DownloadHandler"
FILTER_HANDLER="com.example.lambda.steps.FilterHandler"
ENRICH_HANDLER="com.example.lambda.steps.EnrichHandler"
OUTPUT_HANDLER="com.example.lambda.steps.OutputHandler"

# Disable trailing checksums (AWS CLI v2 feature not supported by LocalStack v2)
export AWS_REQUEST_CHECKSUM_CALCULATION=WHEN_REQUIRED
export AWS_RESPONSE_CHECKSUM_VALIDATION=WHEN_REQUIRED

# Use standard AWS CLI with LocalStack endpoint
AWS_CMD="aws --endpoint-url=$ENDPOINT --region $REGION"

echo "============================================"
echo " LocalStack Step Functions POC Setup"
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

# Deploy step Lambda functions
echo ""
echo "--- Deploying step Lambda functions ---"

deploy_lambda() {
    local FUNC_NAME=$1
    local HANDLER=$2
    local ENV_VARS=${3:-""}

    $AWS_CMD lambda delete-function --function-name $FUNC_NAME 2>/dev/null || true

    if [ -n "$ENV_VARS" ]; then
        $AWS_CMD lambda create-function \
            --function-name $FUNC_NAME \
            --runtime java17 \
            --handler $HANDLER \
            --role arn:aws:iam::000000000000:role/lambda-role \
            --zip-file fileb://$JAR_PATH \
            --timeout 60 \
            --memory-size 512 \
            --environment "Variables={$ENV_VARS}" \
            --no-cli-pager
    else
        $AWS_CMD lambda create-function \
            --function-name $FUNC_NAME \
            --runtime java17 \
            --handler $HANDLER \
            --role arn:aws:iam::000000000000:role/lambda-role \
            --zip-file fileb://$JAR_PATH \
            --timeout 60 \
            --memory-size 512 \
            --no-cli-pager
    fi
    echo "  Deployed: $FUNC_NAME"
}

deploy_lambda "$DOWNLOAD_FUNCTION" "$DOWNLOAD_HANDLER" "AWS_REGION=$REGION"
deploy_lambda "$FILTER_FUNCTION" "$FILTER_HANDLER"
deploy_lambda "$ENRICH_FUNCTION" "$ENRICH_HANDLER"
deploy_lambda "$OUTPUT_FUNCTION" "$OUTPUT_HANDLER" "AWS_REGION=$REGION"

echo "All step functions deployed."

# Wait for functions to be active
echo ""
echo "--- Waiting for Lambda functions to be active ---"
sleep 5

# Get Lambda ARNs
DOWNLOAD_ARN=$($AWS_CMD lambda get-function --function-name $DOWNLOAD_FUNCTION --query 'Configuration.FunctionArn' --output text)
FILTER_ARN=$($AWS_CMD lambda get-function --function-name $FILTER_FUNCTION --query 'Configuration.FunctionArn' --output text)
ENRICH_ARN=$($AWS_CMD lambda get-function --function-name $ENRICH_FUNCTION --query 'Configuration.FunctionArn' --output text)
OUTPUT_ARN=$($AWS_CMD lambda get-function --function-name $OUTPUT_FUNCTION --query 'Configuration.FunctionArn' --output text)

echo "  Download ARN: $DOWNLOAD_ARN"
echo "  Filter ARN:   $FILTER_ARN"
echo "  Enrich ARN:   $ENRICH_ARN"
echo "  Output ARN:   $OUTPUT_ARN"

# Create state machine definition with actual ARNs
echo ""
echo "--- Creating Step Functions state machine ---"

# Replace placeholder ARNs in the ASL definition
STATE_MACHINE_DEF=$(cat state-machine.asl.json \
    | sed "s|\${DownloadHandlerArn}|$DOWNLOAD_ARN|g" \
    | sed "s|\${FilterHandlerArn}|$FILTER_ARN|g" \
    | sed "s|\${EnrichHandlerArn}|$ENRICH_ARN|g" \
    | sed "s|\${OutputHandlerArn}|$OUTPUT_ARN|g")

# Delete existing state machine if present
EXISTING_SM_ARN=$($AWS_CMD stepfunctions list-state-machines --query "stateMachines[?name=='$STATE_MACHINE_NAME'].stateMachineArn" --output text 2>/dev/null || echo "")
if [ -n "$EXISTING_SM_ARN" ] && [ "$EXISTING_SM_ARN" != "None" ]; then
    $AWS_CMD stepfunctions delete-state-machine --state-machine-arn "$EXISTING_SM_ARN" 2>/dev/null || true
    sleep 2
fi

SM_RESULT=$($AWS_CMD stepfunctions create-state-machine \
    --name $STATE_MACHINE_NAME \
    --definition "$STATE_MACHINE_DEF" \
    --role-arn arn:aws:iam::000000000000:role/stepfunctions-role)

STATE_MACHINE_ARN=$(echo "$SM_RESULT" | python3 -c "import sys,json; print(json.load(sys.stdin)['stateMachineArn'])" 2>/dev/null || echo "$SM_RESULT" | grep -o 'arn:aws:states:[^"]*')

echo "State machine created: $STATE_MACHINE_ARN"

# Deploy trigger Lambda
echo ""
echo "--- Deploying trigger Lambda ---"
deploy_lambda "$TRIGGER_FUNCTION" "$TRIGGER_HANDLER" "STATE_MACHINE_ARN=$STATE_MACHINE_ARN,OUTPUT_BUCKET=$OUTPUT_BUCKET,AWS_REGION=$REGION"

# Wait and add S3 permission
sleep 3
echo ""
echo "--- Adding S3 invoke permission to trigger Lambda ---"
$AWS_CMD lambda add-permission \
    --function-name $TRIGGER_FUNCTION \
    --statement-id s3-trigger \
    --action lambda:InvokeFunction \
    --principal s3.amazonaws.com \
    --source-arn arn:aws:s3:::$INPUT_BUCKET \
    --no-cli-pager

# Configure S3 event notification
echo ""
echo "--- Configuring S3 trigger ---"
TRIGGER_ARN=$($AWS_CMD lambda get-function --function-name $TRIGGER_FUNCTION --query 'Configuration.FunctionArn' --output text)

NOTIFICATION_CONFIG='{
  "LambdaFunctionConfigurations": [
    {
      "LambdaFunctionArn": "'"$TRIGGER_ARN"'",
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

echo "S3 trigger configured: $INPUT_BUCKET -> $TRIGGER_FUNCTION -> $STATE_MACHINE_NAME"

echo ""
echo "============================================"
echo " Setup complete!"
echo "============================================"
echo ""
echo "To test, run: ./scripts/send-sample.sh"
echo ""
echo "Useful commands:"
echo "  Upload a file:       $AWS_CMD s3 cp <file.csv> s3://$INPUT_BUCKET/"
echo "  Check output:        $AWS_CMD s3 ls s3://$OUTPUT_BUCKET/"
echo "  List executions:     $AWS_CMD stepfunctions list-executions --state-machine-arn $STATE_MACHINE_ARN"
echo "  Describe execution:  $AWS_CMD stepfunctions describe-execution --execution-arn <arn>"
echo "  View logs:           $AWS_CMD logs filter-log-events --log-group-name /aws/lambda/$TRIGGER_FUNCTION"
