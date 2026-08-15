#!/bin/bash
# ============================================================
# Runs both test scripts back-to-back.
# Both modules must be running:
#   ./gradlew :spring-threading:bootRun   (port 8080)
#   ./gradlew :custom-threading:bootRun   (port 8081)
# ============================================================

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

echo "Running spring-threading tests..."
echo ""
bash "$SCRIPT_DIR/test-spring-threading.sh"

echo ""
echo ""
echo "============================================"
echo ""
echo ""

echo "Running custom-threading tests..."
echo ""
bash "$SCRIPT_DIR/test-custom-threading.sh"
