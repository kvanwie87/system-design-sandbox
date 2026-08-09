# AWS Lambda S3 CSV Processor — Java 17 POC

A proof-of-concept AWS Lambda function (Java 17) that triggers when a CSV file is uploaded to an S3 bucket, processes the file (filters, enriches, converts), and writes the JSON result to an output S3 bucket.

## Architecture

```
┌─────────────────┐         ┌───────────────────────────┐         ┌──────────────────┐
│  S3 Input       │         │     AWS Lambda             │         │  S3 Output       │
│  Bucket         │────────▶│     (Java 17)              │────────▶│  Bucket          │
│                 │ trigger  │                            │  write   │                  │
│  orders.csv     │         │  1. Filter active rows     │         │  orders.json     │
│                 │         │  2. Add "total" column     │         │                  │
│                 │         │  3. Convert to JSON        │         │                  │
└─────────────────┘         └───────────────────────────┘         └──────────────────┘
```

## CSV Processing Logic

Given a CSV with columns `id, product, quantity, status, price`, the Lambda:

1. **Filters** rows where `status == "active"` (case-insensitive)
2. **Enriches** each row with a computed `total = quantity * price`
3. **Converts** the result to a JSON array

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

- **Java 17** (JDK) — [Download from Adoptium](https://adoptium.net/)
- **Docker** and **Docker Compose** — for LocalStack
- **AWS CLI v2** — [Install guide](https://docs.aws.amazon.com/cli/latest/userguide/getting-started-install.html)

Verify your setup:
```bash
java -version     # Should show 17.x
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
- **CsvProcessorServiceTest** — filters active rows, adds total column, handles empty/malformed CSVs, validates against sample data
- **S3CsvProcessorHandlerTest** — mocks S3 client, verifies file download/upload, output key naming, error handling

## Project Structure

```
java-lambda-poc/
├── build.gradle.kts              # Gradle build config (Java 17, Shadow plugin)
├── settings.gradle.kts           # Project name
├── docker-compose.yml            # LocalStack container setup
├── sample-data/
│   ├── orders.csv                # Sample input CSV (10 rows)
│   └── expected-output.json      # Expected JSON output (6 active rows)
├── scripts/
│   ├── localstack-setup.sh       # Deploys Lambda + configures trigger (Linux/macOS)
│   ├── localstack-setup.bat      # Same for Windows
│   ├── localstack-teardown.sh    # Removes all LocalStack resources
│   ├── send-sample.sh            # Uploads sample CSV and checks output (Linux/macOS)
│   └── send-sample.bat           # Same for Windows
└── src/
    ├── main/java/com/example/lambda/
    │   ├── S3CsvProcessorHandler.java  # Lambda handler (S3Event -> process -> S3)
    │   └── CsvProcessorService.java    # Core CSV processing logic
    └── test/java/com/example/lambda/
        ├── CsvProcessorServiceTest.java  # Unit tests for CSV processing
        └── S3CsvProcessorHandlerTest.java # Unit tests for Lambda handler
```

## Local Testing with LocalStack

> **Note:** LocalStack v3 requires a paid `LOCALSTACK_AUTH_TOKEN` for Lambda support. This project
> uses LocalStack v2.3.2 which includes Lambda in the free tier.

### Option A: Docker Compose (Recommended)

This is the fastest way to get the full pipeline running locally.

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
- Deploy the Lambda function
- Configure the S3 event trigger (ObjectCreated, *.csv filter)

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

This uploads `sample-data/orders.csv` to the input bucket, waits for Lambda to process it, and prints the JSON output.

**Step 5: Verify the result**
```bash
# Manually check output bucket
aws --endpoint-url=http://localhost:4566 s3 cp s3://csv-output-bucket/orders.json -
```

**Step 6: Clean up**
```bash
./scripts/localstack-teardown.sh
docker-compose down
```

### Option B: Manual CLI Steps

If you prefer to run each step yourself without Docker Compose:

**Step 1: Start LocalStack**
```bash
docker-compose up -d
```

**Step 2: Disable trailing checksums (required for AWS CLI v2 + LocalStack v2)**
```bash
export AWS_REQUEST_CHECKSUM_CALCULATION=WHEN_REQUIRED
export AWS_RESPONSE_CHECKSUM_VALIDATION=WHEN_REQUIRED
```

**Step 3: Wait for LocalStack to be ready**
```bash
curl http://localhost:4566/_localstack/health
# Verify "s3" and "lambda" show "available" or "running"
```

**Step 4: Create S3 buckets**
```bash
aws --endpoint-url=http://localhost:4566 s3 mb s3://csv-input-bucket
aws --endpoint-url=http://localhost:4566 s3 mb s3://csv-output-bucket
```

**Step 5: Build and deploy the Lambda**
```bash
./gradlew shadowJar

aws --endpoint-url=http://localhost:4566 --region us-east-1 \
    lambda create-function \
    --function-name csv-processor \
    --runtime java17 \
    --handler com.example.lambda.S3CsvProcessorHandler \
    --role arn:aws:iam::000000000000:role/lambda-role \
    --zip-file fileb://build/libs/java-lambda-poc-all.jar \
    --timeout 60 \
    --memory-size 512 \
    --environment "Variables={OUTPUT_BUCKET=csv-output-bucket}"
```

**Step 6: Add permission for S3 to invoke the Lambda**
```bash
aws --endpoint-url=http://localhost:4566 --region us-east-1 \
    lambda add-permission \
    --function-name csv-processor \
    --statement-id s3-trigger \
    --action lambda:InvokeFunction \
    --principal s3.amazonaws.com \
    --source-arn arn:aws:s3:::csv-input-bucket
```

**Step 7: Configure S3 event notification**
```bash
LAMBDA_ARN=$(aws --endpoint-url=http://localhost:4566 --region us-east-1 \
    lambda get-function \
    --function-name csv-processor \
    --query 'Configuration.FunctionArn' \
    --output text)

aws --endpoint-url=http://localhost:4566 --region us-east-1 \
    s3api put-bucket-notification-configuration \
    --bucket csv-input-bucket \
    --notification-configuration '{
      "LambdaFunctionConfigurations": [{
        "LambdaFunctionArn": "'"$LAMBDA_ARN"'",
        "Events": ["s3:ObjectCreated:*"],
        "Filter": {
          "Key": {
            "FilterRules": [{"Name": "suffix", "Value": ".csv"}]
          }
        }
      }]
    }'
```

**Step 8: Test by uploading a CSV**
```bash
aws --endpoint-url=http://localhost:4566 s3 cp sample-data/orders.csv s3://csv-input-bucket/orders.csv
```

**Step 9: Check the output (wait ~10 seconds for Lambda execution)**
```bash
sleep 10
aws --endpoint-url=http://localhost:4566 s3 cp s3://csv-output-bucket/orders.json -
```

**Step 10: Clean up**
```bash
aws --endpoint-url=http://localhost:4566 --region us-east-1 \
    lambda delete-function --function-name csv-processor
aws --endpoint-url=http://localhost:4566 s3 rm s3://csv-input-bucket --recursive
aws --endpoint-url=http://localhost:4566 s3 rb s3://csv-input-bucket
aws --endpoint-url=http://localhost:4566 s3 rm s3://csv-output-bucket --recursive
aws --endpoint-url=http://localhost:4566 s3 rb s3://csv-output-bucket
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

### Step 2: Create IAM Execution Role

1. Open the [IAM Console > Roles](https://console.aws.amazon.com/iam/home#/roles)
2. Click **Create role**
3. Select **AWS service** as trusted entity, then **Lambda** as use case
4. Click **Next**
5. Attach the following policies:
   - `AWSLambdaBasicExecutionRole` (for CloudWatch Logs)
6. Click **Next**, name the role: `lambda-s3-csv-processor-role`
7. Click **Create role**
8. Open the newly created role and click **Add permissions > Create inline policy**
9. Switch to the **JSON** tab and paste:

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
    }
  ]
}
```

10. Name the policy `s3-csv-processor-access` and click **Create policy**

### Step 3: Create the Lambda Function

1. Open the [Lambda Console](https://console.aws.amazon.com/lambda/home)
2. Click **Create function**
3. Select **Author from scratch**
4. Configure:
   - **Function name**: `csv-processor`
   - **Runtime**: Java 17
   - **Architecture**: x86_64
   - **Permissions**: Use existing role → `lambda-s3-csv-processor-role`
5. Click **Create function**
6. In the **Code** tab, click **Upload from** > **.zip or .jar file**
7. Upload `build/libs/java-lambda-poc-all.jar`
8. In the **Runtime settings** section, click **Edit**:
   - **Handler**: `com.example.lambda.S3CsvProcessorHandler`
9. Click **Save**

### Step 4: Configure Environment Variable

1. In the Lambda function, go to **Configuration** > **Environment variables**
2. Click **Edit** > **Add environment variable**
3. Set:
   - **Key**: `OUTPUT_BUCKET`
   - **Value**: `csv-output-bucket-<your-account-id>`
4. Click **Save**

### Step 5: Increase Timeout and Memory

1. Go to **Configuration** > **General configuration**
2. Click **Edit**
3. Set:
   - **Memory**: 512 MB
   - **Timeout**: 1 min 0 sec
4. Click **Save**

### Step 6: Add S3 Trigger

1. In the Lambda function, click **Add trigger**
2. Select **S3** as the trigger source
3. Configure:
   - **Bucket**: `csv-input-bucket-<your-account-id>`
   - **Event types**: `All object create events`
   - **Prefix**: (leave empty)
   - **Suffix**: `.csv`
4. Check the **Recursive invocation** acknowledgment
5. Click **Add**

### Step 7: Test

1. Open the [S3 Console](https://s3.console.aws.amazon.com/s3/home)
2. Navigate to your input bucket
3. Click **Upload** and upload `sample-data/orders.csv`
4. Wait ~10 seconds
5. Navigate to your output bucket — you should see `orders.json`
6. Download and verify the JSON contains only active rows with the `total` field

### Troubleshooting

If the output file doesn't appear:

1. Check **CloudWatch Logs** for the Lambda function:
   - Go to [CloudWatch Console](https://console.aws.amazon.com/cloudwatch/home) > **Log groups**
   - Find `/aws/lambda/csv-processor`
   - Check the latest log stream for errors
2. Common issues:
   - **Timeout**: Increase Lambda timeout if processing large files
   - **Permission denied on GetObject**: Verify the IAM role has `s3:GetObject` on the input bucket
   - **Permission denied on PutObject**: Verify the IAM role has `s3:PutObject` on the output bucket
   - **Handler not found**: Verify the handler is set to `com.example.lambda.S3CsvProcessorHandler`
   - **OUTPUT_BUCKET not set**: Check the environment variable configuration

## Customization

### Change filter criteria

Edit `CsvProcessorService.java`:
```java
// Change from active-only to a different status
private static final String ACTIVE_STATUS = "active";

// Or modify the filter logic in processCSV():
if (statusIndex >= 0 && !ACTIVE_STATUS.equalsIgnoreCase(line[statusIndex].trim())) {
    continue;
}
```

### Add different enrichment columns

In `CsvProcessorService.java`, after the `computeTotal` call:
```java
// Add a timestamp column
record.put("processed_at", java.time.Instant.now().toString());

// Add a discount column
record.put("discount", quantity > 10 ? "10%" : "0%");
```

### Change input format

The handler processes any CSV with headers. Adjust the column constants in `CsvProcessorService.java`:
```java
private static final String STATUS_COLUMN = "status";
private static final String QUANTITY_COLUMN = "quantity";
private static final String PRICE_COLUMN = "price";
```

## Technology Stack

| Component | Technology |
|-----------|-----------|
| Runtime | Java 17 |
| Build | Gradle 8.7 + Shadow plugin |
| AWS SDK | AWS SDK for Java v1 |
| CSV parsing | OpenCSV 5.9 |
| JSON | Jackson Databind 2.17 |
| Lambda libs | aws-lambda-java-core, aws-lambda-java-events |
| Local testing | LocalStack (Docker) |
