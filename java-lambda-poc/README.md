# AWS Lambda + Step Functions S3 CSV Processor — Java 17 POC

A proof-of-concept using AWS Step Functions to orchestrate a CSV processing pipeline. When a CSV file is uploaded to an S3 bucket, a Step Functions state machine is triggered that downloads, filters, enriches, and outputs the result as JSON to another S3 bucket.

## Architecture

```
┌─────────────────┐      ┌─────────────────┐      ┌────────────────────────────────────────────────┐
│  S3 Input       │      │  Trigger Lambda  │      │  Step Functions State Machine                   │
│  Bucket         │─────▶│                  │─────▶│                                                │
│                 │  S3   │  Starts state    │      │  ┌──────────┐  ┌────────┐  ┌────────┐  ┌────┐ │
│  orders.csv     │ event │  machine         │      │  │ Download │─▶│ Filter │─▶│ Enrich │─▶│Out │ │
└─────────────────┘      └─────────────────┘      │  └──────────┘  └────────┘  └────────┘  └────┘ │
                                                   └────────────────────────────────────────────────┘
                                                                                       │
                                                                                       ▼
                                                                            ┌──────────────────┐
                                                                            │  S3 Output       │
                                                                            │  Bucket          │
                                                                            │  orders.json     │
                                                                            └──────────────────┘
```

### Pipeline Steps

| Step | Lambda | Description |
|------|--------|-------------|
| 1 | `csv-download` | Downloads CSV from S3, parses into rows |
| 2 | `csv-filter` | Keeps only rows where `status == "active"` |
| 3 | `csv-enrich` | Adds computed `total = quantity * price` |
| 4 | `csv-output` | Converts to JSON array, uploads to output S3 bucket |

A separate `csv-trigger` Lambda receives the S3 event and starts the state machine execution.

### State Machine Flow

```
DownloadAndParse ──▶ CheckDownloadStatus
                          │
                    ┌─────┼─────────┐
                    ▼     ▼         ▼
                 ERROR  EMPTY    FilterRows
                   │      │         │
                   ▼      ▼         ▼
              Failed   Succeed   EnrichRows
                                    │
                                    ▼
                                WriteOutput ──▶ CheckOutputStatus
                                                      │
                                                ┌─────┼─────┐
                                                ▼           ▼
                                             ERROR      Succeed
                                               │
                                               ▼
                                            Failed
```

## CSV Processing Logic

Given a CSV with columns `id, product, quantity, status, price`, the pipeline:

1. **Downloads** the CSV from S3 and parses it into header + row maps
2. **Filters** rows where `status == "active"` (case-insensitive)
3. **Enriches** each row with a computed `total = quantity * price`
4. **Outputs** the result as a JSON array to the output S3 bucket

### Example

**Input** (`orders.csv`):
```csv
id,product,quantity,status,price
1,Widget A,10,active,5.99
2,Widget B,3,inactive,12.50
3,Gadget C,7,active,8.75
```

**Output** (`orders.json`):
```json
[ {
  "id" : "1",
  "product" : "Widget A",
  "quantity" : "10",
  "status" : "active",
  "price" : "5.99",
  "total" : "59.90"
}, {
  "id" : "3",
  "product" : "Gadget C",
  "quantity" : "7",
  "status" : "active",
  "price" : "8.75",
  "total" : "61.25"
} ]
```

## Prerequisites

- **Java 17+** (JDK) — [Download from Adoptium](https://adoptium.net/)
- **Docker** and **Docker Compose** — for LocalStack
- **AWS CLI v2** — [Install guide](https://docs.aws.amazon.com/cli/latest/userguide/getting-started-install.html)

Verify your setup:
```bash
java -version     # Should show 17.x or 21.x
docker --version  # Docker 20+
aws --version     # AWS CLI 2.x
```

## Build

```bash
# Linux/macOS
./gradlew shadowJar

# Windows
gradlew.bat shadowJar
```

The fat JAR is output to: `build/libs/java-lambda-poc-all.jar`

## Run Unit Tests

```bash
# Linux/macOS
./gradlew test

# Windows
gradlew.bat test
```

Tests cover:
- **DownloadHandlerTest** — parses CSV, handles empty files and S3 errors
- **FilterHandlerTest** — filters active rows, case-insensitive, handles edge cases
- **EnrichHandlerTest** — adds total column, handles non-numeric values
- **OutputHandlerTest** — uploads JSON to S3, derives output key, handles errors
- **StepFunctionTriggerHandlerTest** — starts state machine, passes correct input
- **CsvProcessorServiceTest** — end-to-end CSV processing (legacy single-Lambda tests)
- **S3CsvProcessorHandlerTest** — legacy single-Lambda handler tests

## Project Structure

```
java-lambda-poc/
├── build.gradle.kts              # Gradle build config (Java 17, Shadow plugin)
├── settings.gradle.kts           # Project name
├── docker-compose.yml            # LocalStack container setup
├── state-machine.asl.json        # Step Functions state machine definition (ASL)
├── sample-data/
│   ├── orders.csv                # Sample input CSV (10 rows)
│   └── expected-output.json      # Expected JSON output (6 active rows)
├── scripts/
│   ├── localstack-setup.sh       # Deploys Lambdas + state machine + trigger (Linux/macOS)
│   ├── localstack-setup.bat      # Same for Windows
│   ├── localstack-teardown.sh    # Removes all LocalStack resources
│   ├── send-sample.sh            # Uploads sample CSV and checks output (Linux/macOS)
│   └── send-sample.bat           # Same for Windows
└── src/
    ├── main/java/com/example/lambda/
    │   ├── StepFunctionTriggerHandler.java  # S3 event → starts state machine
    │   ├── S3CsvProcessorHandler.java       # Legacy single-Lambda handler
    │   ├── CsvProcessorService.java         # Legacy monolithic CSV processor
    │   ├── model/
    │   │   └── PipelineState.java           # Shared state between steps
    │   ├── steps/
    │   │   ├── DownloadHandler.java         # Step 1: Download & parse CSV
    │   │   ├── FilterHandler.java           # Step 2: Filter active rows
    │   │   ├── EnrichHandler.java           # Step 3: Add total column
    │   │   └── OutputHandler.java           # Step 4: Write JSON to S3
    │   └── util/
    │       └── S3ClientFactory.java         # S3 client (LocalStack-aware)
    └── test/java/com/example/lambda/
        ├── CsvProcessorServiceTest.java
        ├── S3CsvProcessorHandlerTest.java
        ├── StepFunctionTriggerHandlerTest.java
        └── steps/
            ├── DownloadHandlerTest.java
            ├── FilterHandlerTest.java
            ├── EnrichHandlerTest.java
            └── OutputHandlerTest.java
```

## Local Testing with LocalStack

### Option A: Docker Compose (Recommended)

> **Note:** Uses LocalStack v2.3.2 which includes Lambda and Step Functions in the free tier.
> AWS CLI v2 trailing checksums are disabled automatically by the scripts.

**Step 1: Build the shadow JAR**
```bash
./gradlew shadowJar
```

**Step 2: Start LocalStack**
```bash
docker-compose up -d
```

**Step 3: Run the setup script**

Linux/macOS:
```bash
chmod +x scripts/localstack-setup.sh
./scripts/localstack-setup.sh
```

Windows:
```cmd
scripts\localstack-setup.bat
```

The script will:
- Create input and output S3 buckets
- Deploy 5 Lambda functions (4 steps + 1 trigger)
- Create the Step Functions state machine
- Configure S3 event notification to trigger the pipeline

**Step 4: Send sample input and verify output**

Linux/macOS:
```bash
chmod +x scripts/send-sample.sh
./scripts/send-sample.sh
```

Windows:
```cmd
scripts\send-sample.bat
```

This uploads `sample-data/orders.csv`, waits for the pipeline to complete, and prints the JSON output.

**Step 5: Check Step Functions execution**
```bash
# Set env vars for AWS CLI v2 compatibility
export AWS_REQUEST_CHECKSUM_CALCULATION=WHEN_REQUIRED
export AWS_RESPONSE_CHECKSUM_VALIDATION=WHEN_REQUIRED

# List executions
aws --endpoint-url=http://localhost:4566 --region us-east-1 \
    stepfunctions list-executions \
    --state-machine-arn arn:aws:states:us-east-1:000000000000:stateMachine:csv-pipeline
```

**Step 6: Clean up**
```bash
./scripts/localstack-teardown.sh
docker-compose down
```

### Option B: Manual CLI Steps

**Step 1: Start LocalStack**
```bash
docker-compose up -d
```

**Step 2: Set environment variables**
```bash
export AWS_REQUEST_CHECKSUM_CALCULATION=WHEN_REQUIRED
export AWS_RESPONSE_CHECKSUM_VALIDATION=WHEN_REQUIRED
export ENDPOINT=http://localhost:4566
export REGION=us-east-1
alias awsl="aws --endpoint-url=$ENDPOINT --region $REGION"
```

**Step 3: Create S3 buckets**
```bash
awsl s3 mb s3://csv-input-bucket
awsl s3 mb s3://csv-output-bucket
```

**Step 4: Build and deploy Lambda functions**
```bash
./gradlew shadowJar

# Deploy step functions
awsl lambda create-function --function-name csv-download \
    --runtime java17 --handler com.example.lambda.steps.DownloadHandler \
    --role arn:aws:iam::000000000000:role/lambda-role \
    --zip-file fileb://build/libs/java-lambda-poc-all.jar \
    --timeout 60 --memory-size 512 \
    --environment "Variables={AWS_REGION=us-east-1}"

awsl lambda create-function --function-name csv-filter \
    --runtime java17 --handler com.example.lambda.steps.FilterHandler \
    --role arn:aws:iam::000000000000:role/lambda-role \
    --zip-file fileb://build/libs/java-lambda-poc-all.jar \
    --timeout 60 --memory-size 512

awsl lambda create-function --function-name csv-enrich \
    --runtime java17 --handler com.example.lambda.steps.EnrichHandler \
    --role arn:aws:iam::000000000000:role/lambda-role \
    --zip-file fileb://build/libs/java-lambda-poc-all.jar \
    --timeout 60 --memory-size 512

awsl lambda create-function --function-name csv-output \
    --runtime java17 --handler com.example.lambda.steps.OutputHandler \
    --role arn:aws:iam::000000000000:role/lambda-role \
    --zip-file fileb://build/libs/java-lambda-poc-all.jar \
    --timeout 60 --memory-size 512 \
    --environment "Variables={AWS_REGION=us-east-1}"
```

**Step 5: Get Lambda ARNs and create state machine**
```bash
DOWNLOAD_ARN=$(awsl lambda get-function --function-name csv-download --query Configuration.FunctionArn --output text)
FILTER_ARN=$(awsl lambda get-function --function-name csv-filter --query Configuration.FunctionArn --output text)
ENRICH_ARN=$(awsl lambda get-function --function-name csv-enrich --query Configuration.FunctionArn --output text)
OUTPUT_ARN=$(awsl lambda get-function --function-name csv-output --query Configuration.FunctionArn --output text)

# Create state machine with resolved ARNs
SM_DEF=$(cat state-machine.asl.json \
    | sed "s|\${DownloadHandlerArn}|$DOWNLOAD_ARN|g" \
    | sed "s|\${FilterHandlerArn}|$FILTER_ARN|g" \
    | sed "s|\${EnrichHandlerArn}|$ENRICH_ARN|g" \
    | sed "s|\${OutputHandlerArn}|$OUTPUT_ARN|g")

awsl stepfunctions create-state-machine \
    --name csv-pipeline \
    --definition "$SM_DEF" \
    --role-arn arn:aws:iam::000000000000:role/stepfunctions-role
```

**Step 6: Deploy trigger Lambda**
```bash
STATE_MACHINE_ARN=$(awsl stepfunctions list-state-machines \
    --query "stateMachines[?name=='csv-pipeline'].stateMachineArn" --output text)

awsl lambda create-function --function-name csv-trigger \
    --runtime java17 --handler com.example.lambda.StepFunctionTriggerHandler \
    --role arn:aws:iam::000000000000:role/lambda-role \
    --zip-file fileb://build/libs/java-lambda-poc-all.jar \
    --timeout 60 --memory-size 512 \
    --environment "Variables={STATE_MACHINE_ARN=$STATE_MACHINE_ARN,OUTPUT_BUCKET=csv-output-bucket,AWS_REGION=us-east-1}"
```

**Step 7: Configure S3 trigger**
```bash
awsl lambda add-permission --function-name csv-trigger \
    --statement-id s3-trigger --action lambda:InvokeFunction \
    --principal s3.amazonaws.com --source-arn arn:aws:s3:::csv-input-bucket

TRIGGER_ARN=$(awsl lambda get-function --function-name csv-trigger --query Configuration.FunctionArn --output text)

awsl s3api put-bucket-notification-configuration --bucket csv-input-bucket \
    --notification-configuration '{
      "LambdaFunctionConfigurations": [{
        "LambdaFunctionArn": "'"$TRIGGER_ARN"'",
        "Events": ["s3:ObjectCreated:*"],
        "Filter": {"Key": {"FilterRules": [{"Name": "suffix", "Value": ".csv"}]}}
      }]
    }'
```

**Step 8: Test**
```bash
awsl s3 cp sample-data/orders.csv s3://csv-input-bucket/orders.csv
sleep 15
awsl s3 cp s3://csv-output-bucket/orders.json -
```

**Step 9: Check execution history**
```bash
awsl stepfunctions list-executions --state-machine-arn $STATE_MACHINE_ARN
```

**Step 10: Clean up**
```bash
./scripts/localstack-teardown.sh
docker-compose down
```

## AWS Console Setup

Follow these steps to deploy the POC to a real AWS account.

### Step 1: Create S3 Buckets

1. Open the [S3 Console](https://s3.console.aws.amazon.com/s3/home)
2. Click **Create bucket**
3. Enter bucket name: `csv-input-bucket-<your-account-id>` (must be globally unique)
4. Select your preferred region (e.g., `us-east-1`)
5. Leave all other settings as default, click **Create bucket**
6. Repeat for the output bucket: `csv-output-bucket-<your-account-id>`

### Step 2: Create IAM Roles

You need two IAM roles: one for Lambda functions and one for the Step Functions state machine.

**Lambda Execution Role:**

1. Open the [IAM Console > Roles](https://console.aws.amazon.com/iam/home#/roles)
2. Click **Create role** → **AWS service** → **Lambda** → **Next**
3. Attach: `AWSLambdaBasicExecutionRole`
4. Name: `lambda-csv-pipeline-role` → **Create role**
5. Open the role → **Add permissions** → **Create inline policy** → JSON:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": ["s3:GetObject"],
      "Resource": "arn:aws:s3:::csv-input-bucket-<your-account-id>/*"
    },
    {
      "Effect": "Allow",
      "Action": ["s3:PutObject"],
      "Resource": "arn:aws:s3:::csv-output-bucket-<your-account-id>/*"
    },
    {
      "Effect": "Allow",
      "Action": ["states:StartExecution"],
      "Resource": "*"
    }
  ]
}
```

6. Name: `csv-pipeline-permissions` → **Create policy**

**Step Functions Execution Role:**

1. Click **Create role** → **AWS service** → **Step Functions** → **Next**
2. Attach: `AWSLambdaRole` (allows invoking Lambda functions)
3. Name: `stepfunctions-csv-pipeline-role` → **Create role**

### Step 3: Create the Lambda Functions

1. Open the [Lambda Console](https://console.aws.amazon.com/lambda/home)
2. Create 5 functions with these settings:

| Function Name | Handler | Env Vars |
|--------------|---------|----------|
| `csv-download` | `com.example.lambda.steps.DownloadHandler` | — |
| `csv-filter` | `com.example.lambda.steps.FilterHandler` | — |
| `csv-enrich` | `com.example.lambda.steps.EnrichHandler` | — |
| `csv-output` | `com.example.lambda.steps.OutputHandler` | — |
| `csv-trigger` | `com.example.lambda.StepFunctionTriggerHandler` | (set after state machine is created) |

For each function:
- **Runtime**: Java 17
- **Architecture**: x86_64
- **Role**: `lambda-csv-pipeline-role`
- **Memory**: 512 MB
- **Timeout**: 1 min
- **Code**: Upload `build/libs/java-lambda-poc-all.jar`

### Step 4: Create the Step Functions State Machine

1. Open the [Step Functions Console](https://console.aws.amazon.com/states/home)
2. Click **Create state machine**
3. Select **Write your workflow in code**
4. Paste the contents of `state-machine.asl.json`
5. Replace the `${...Arn}` placeholders with the actual Lambda ARNs from Step 3
6. Name: `csv-pipeline`
7. **Execution role**: `stepfunctions-csv-pipeline-role`
8. Click **Create state machine**

### Step 5: Configure the Trigger Lambda

1. Open the `csv-trigger` function in the Lambda Console
2. Go to **Configuration** > **Environment variables**
3. Add:
   - `STATE_MACHINE_ARN`: (copy ARN from the Step Functions console)
   - `OUTPUT_BUCKET`: `csv-output-bucket-<your-account-id>`

### Step 6: Add S3 Trigger

1. In the `csv-trigger` function, click **Add trigger**
2. Select **S3**
3. Configure:
   - **Bucket**: `csv-input-bucket-<your-account-id>`
   - **Event types**: `All object create events`
   - **Suffix**: `.csv`
4. Check the recursive invocation acknowledgment
5. Click **Add**

### Step 7: Test

1. Upload `sample-data/orders.csv` to the input bucket
2. Open the Step Functions console → find the `csv-pipeline` state machine
3. You should see a new execution in **Running** or **Succeeded** state
4. Click into the execution to see each step's input/output
5. Check the output bucket for `orders.json`

### Troubleshooting

- **Execution shows "Failed"**: Click the failed step to see error details in the Step Functions console
- **Trigger Lambda not firing**: Check S3 event notification configuration and Lambda permissions
- **Download step fails**: Verify Lambda role has `s3:GetObject` on the input bucket
- **Output step fails**: Verify Lambda role has `s3:PutObject` on the output bucket
- **State machine not starting**: Check trigger Lambda's `STATE_MACHINE_ARN` env var and that the role has `states:StartExecution`

## Customization

### Change filter criteria

Edit `FilterHandler.java`:
```java
private static final String ACTIVE_STATUS = "active";
```

### Add different enrichment columns

Edit `EnrichHandler.java`:
```java
// Add a discount field
row.put("discount", quantity > 10 ? "10%" : "0%");
```

### Add a new step

1. Create a new handler class implementing `RequestHandler<PipelineState, PipelineState>`
2. Add it as a new state in `state-machine.asl.json`
3. Deploy the new Lambda and update the state machine

### Add error handling with retries

In `state-machine.asl.json`, add a `Retry` clause to any Task state:
```json
"Retry": [
  {
    "ErrorEquals": ["States.TaskFailed"],
    "IntervalSeconds": 3,
    "MaxAttempts": 2,
    "BackoffRate": 2.0
  }
]
```

## Technology Stack

| Component | Technology |
|-----------|-----------|
| Runtime | Java 17 |
| Build | Gradle 8.7 + Shadow plugin |
| AWS SDK | AWS SDK for Java v1 |
| Orchestration | AWS Step Functions |
| CSV parsing | OpenCSV 5.9 |
| JSON | Jackson Databind 2.17 |
| Lambda libs | aws-lambda-java-core, aws-lambda-java-events |
| Local testing | LocalStack 2.3.2 (Docker) |
