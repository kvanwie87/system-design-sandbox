#!/bin/bash
# ============================================================
# Local test script: Builds the project and runs the CSV
# processor against sample data to verify output.
# ============================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

echo "=== Building shadow JAR ==="
cd "$PROJECT_DIR"
./gradlew shadowJar --quiet

echo ""
echo "=== Running CSV processor against sample data ==="
java -cp build/libs/java-lambda-poc-all.jar \
    com.example.lambda.LocalTestRunner \
    sample-data/orders.csv \
    build/test-output.json

echo ""
echo "=== Output ==="
cat build/test-output.json

echo ""
echo ""
echo "=== Comparing with expected output ==="
# Normalize whitespace for comparison
EXPECTED=$(python3 -c "import json,sys; print(json.dumps(json.load(open(sys.argv[1])),sort_keys=True))" sample-data/expected-output.json 2>/dev/null || cat sample-data/expected-output.json)
ACTUAL=$(python3 -c "import json,sys; print(json.dumps(json.load(open(sys.argv[1])),sort_keys=True))" build/test-output.json 2>/dev/null || cat build/test-output.json)

if [ "$EXPECTED" = "$ACTUAL" ]; then
    echo "SUCCESS: Output matches expected result!"
    exit 0
else
    echo "MISMATCH: Output differs from expected result."
    echo ""
    echo "Expected:"
    echo "$EXPECTED"
    echo ""
    echo "Actual:"
    echo "$ACTUAL"
    exit 1
fi
