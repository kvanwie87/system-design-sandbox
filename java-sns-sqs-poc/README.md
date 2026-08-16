# Java POC: S3 → Lambda → SNS → 2 SQS

Event-driven pipeline demonstrating S3 file processing with fan-out messaging using AWS Lambda, SNS, and SQS.

## Architecture

```
┌─────────────┐    ObjectCreated    ┌─────────────────┐    Publish    ┌──────────────┐
│  S3 Bucket  │ ──────────────────> │  Lambda         │ ───────────> │  SNS Topic   │
│  csv-input  │                     │  csv-processor  │              │  csv-processed│
└─────────────┘                     └─────────────────┘              └──────┬───────┘
                                          │                                 │
                                          │ GetObject                       │ Subscribe
                                          ▼                                 │
                                    ┌─────────────┐              ┌─────────┴────────┐
                                    │  S3 Bucket  │              │                  │
                                    │  (read CSV) │         ┌────▼─────┐     ┌─────▼──────┐
                                    └─────────────┘         │  SQS     │     │  SQS       │
                                                            │  audit   │     │  notification│
                                                            └──────────┘     └────────────┘
```

**Flow:**
1. A CSV file is uploaded to the S3 bucket
2. S3 triggers the Lambda function
3. Lambda downloads and parses the CSV, producing a summary (row count, columns, first few rows)
4. Lambda publishes the summary as JSON to an SNS topic
5. SNS fans out the message to two SQS queues: `audit-queue` and `notification-queue`

## Prerequisites

- **Java 17+** (build uses Java 17 target)
- **Gradle 8.7** (wrapper included)
- **Docker** and **Docker Compose** (for LocalStack)
- **AWS CLI v2**
- **PowerShell** (required by Windows `.bat` scripts for JSON generation)

## Project Structure

```
├── build.gradle                    # Gradle build with Shadow JAR plugin
├── settings.gradle
├── docker-compose.yml              # LocalStack v2 container
├── samples/
│   └── test.csv                    # Sample CSV for testing
├── scripts/
│   ├── localstack/
│   │   ├── setup.sh                # Create resources on LocalStack
│   │   ├── setup.bat               # Create resources on LocalStack (Windows)
│   │   ├── test-e2e.sh             # End-to-end test on LocalStack
│   │   └── test-e2e.bat            # End-to-end test on LocalStack (Windows)
│   └── aws/
│       ├── setup.sh                # Create resources on real AWS
│       ├── setup.bat               # Create resources on real AWS (Windows)
│       ├── teardown.sh             # Clean up AWS resources
│       └── teardown.bat            # Clean up AWS resources (Windows)
└── src/
    ├── main/java/com/example/processor/
    │   ├── CsvProcessorHandler.java  # Lambda entry point
    │   ├── CsvParser.java            # CSV parsing utility
    │   ├── CsvSummary.java           # Summary POJO
    │   └── SnsPublisher.java         # SNS publishing service
    └── test/java/com/example/processor/
        ├── CsvParserTest.java
        ├── CsvProcessorHandlerTest.java
        └── SnsPublisherTest.java
```

## Local Development (LocalStack)

### 1. Build the project

```bash
./gradlew shadowJar
```

This produces `build/libs/csv-processor-lambda-all.jar`.

### 2. Start LocalStack

```bash
docker-compose up -d
```

Wait for the container to be healthy:
```bash
docker-compose ps   # Should show "healthy"
```

### 3. Set up infrastructure

```bash
./scripts/localstack/setup.sh                # Linux/Mac/Git Bash
./scripts/localstack/setup.sh --profile foo  # with a specific AWS CLI profile
scripts\localstack\setup.bat                 # Windows Command Prompt
scripts\localstack\setup.bat --profile foo   # Windows with profile
```

This creates the S3 bucket (`csv-input-bucket`), SNS topic, SQS queues, subscriptions, IAM role, Lambda function, and S3 event notification.

### 4. Run end-to-end test

```bash
./scripts/localstack/test-e2e.sh     # Linux/Mac/Git Bash
scripts\localstack\test-e2e.bat      # Windows Command Prompt
```

This uploads `samples/test.csv` to S3, waits for Lambda processing, and verifies messages arrive in both SQS queues.

### 5. Manual testing

```bash
# Upload a file
aws --endpoint-url=http://localhost:4566 --region us-east-1 s3 cp samples/test.csv s3://csv-input-bucket/

# Wait a few seconds, then check queues
aws --endpoint-url=http://localhost:4566 --region us-east-1 sqs receive-message \
    --queue-url http://sqs.us-east-1.localhost.localstack.cloud:4566/000000000000/audit-queue

aws --endpoint-url=http://localhost:4566 --region us-east-1 sqs receive-message \
    --queue-url http://sqs.us-east-1.localhost.localstack.cloud:4566/000000000000/notification-queue
```

### 6. Viewing Lambda logs

LocalStack community edition does not fully support CloudWatch Logs. View Lambda output via Docker container logs:

```bash
# Follow logs in real-time
docker logs -f localstack

# Filter for Lambda output
docker logs localstack 2>&1 | grep -i "Processing\|Published\|ERROR"
```

### 7. Stop LocalStack

```bash
docker-compose down
```

## AWS Deployment

### 1. Build the project

```bash
./gradlew shadowJar
```

### 2. Configure AWS CLI

Ensure your AWS CLI is configured with credentials that have permissions to create S3, SNS, SQS, Lambda, and IAM resources:

```bash
aws configure
# Or set: AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY, AWS_REGION
```

### 3. Deploy infrastructure

```bash
# Optionally set a region (defaults to us-east-1)
export AWS_REGION=us-east-1

./scripts/aws/setup.sh                       # Linux/Mac/Git Bash
./scripts/aws/setup.sh --profile my-profile  # with a specific AWS CLI profile
scripts\aws\setup.bat                        # Windows Command Prompt
scripts\aws\setup.bat --profile my-profile   # Windows with profile
```

The S3 bucket is named `csv-input-bucket-<account-id>` to ensure global uniqueness.

### 4. Test

```bash
# The bucket name includes your account ID (shown in setup output)
aws s3 cp samples/test.csv s3://csv-input-bucket-<account-id>/

# Wait ~10 seconds for processing, then check queues
aws sqs receive-message --queue-url <audit-queue-url-from-setup-output>
aws sqs receive-message --queue-url <notification-queue-url-from-setup-output>
```

### 5. Cleanup

```bash
./scripts/aws/teardown.sh                       # Linux/Mac/Git Bash
./scripts/aws/teardown.sh --profile my-profile  # with profile
scripts\aws\teardown.bat                        # Windows Command Prompt
scripts\aws\teardown.bat --profile my-profile   # Windows with profile
```

This removes all created resources (Lambda, SNS, SQS, S3, IAM role).

## Key Design Decisions

- **Endpoint override via env var**: The Lambda handler checks `AWS_ENDPOINT_URL` at startup. If set (LocalStack), it overrides the SDK endpoint. On real AWS, this env var is absent, so default endpoints are used.
- **S3 path-style access**: `forcePathStyle(true)` is always enabled on the S3 client for LocalStack compatibility. This works fine on real AWS as well.
- **Shadow JAR**: All dependencies are bundled into a single JAR for Lambda deployment (no Lambda layers needed).
- **No external CSV library**: Simple `String.split(",")` parsing for POC purposes. For production, consider a library like OpenCSV or Apache Commons CSV.
- **Unique bucket names on AWS**: The AWS scripts append your account ID to the bucket name (`csv-input-bucket-<account-id>`) since S3 bucket names are globally unique. LocalStack scripts use `csv-input-bucket` (local only).
- **Profile support**: All scripts accept `--profile <name>` to target a specific AWS CLI profile.

## Running Tests

```bash
./gradlew test
```

Unit tests use Mockito to mock AWS SDK clients, verifying the handler logic without requiring any AWS infrastructure.
