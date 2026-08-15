#!/usr/bin/env bash
#
# Validates that sensor readings are being persisted to PostgreSQL.
#
# Usage:
#   ./scripts/check-db.sh              # Summary + latest 10 readings
#   ./scripts/check-db.sh --count      # Just the total count
#   ./scripts/check-db.sh --watch      # Poll every 2 seconds showing new counts
#   ./scripts/check-db.sh --sensors    # Per-sensor breakdown
#   ./scripts/check-db.sh --alerts     # Show readings that exceeded threshold

set -euo pipefail

DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-5432}"
DB_NAME="${DB_NAME:-telemetry}"
DB_USER="${DB_USER:-telemetry}"
DB_PASS="${DB_PASS:-telemetry}"

COMMAND="${1:---summary}"

psql_cmd() {
    PGPASSWORD="$DB_PASS" docker exec -i postgres psql -U "$DB_USER" -d "$DB_NAME" -t -A -c "$1" 2>/dev/null
}

psql_pretty() {
    PGPASSWORD="$DB_PASS" docker exec -i postgres psql -U "$DB_USER" -d "$DB_NAME" -c "$1" 2>/dev/null
}

check_connection() {
    if ! docker exec postgres pg_isready -U "$DB_USER" > /dev/null 2>&1; then
        echo "ERROR: Cannot connect to PostgreSQL. Is the container running?"
        echo "  Run: docker compose up -d postgres"
        exit 1
    fi
}

case "$COMMAND" in
    --summary|-s)
        check_connection
        echo "=== Database Validation ==="
        echo ""
        TOTAL=$(psql_cmd "SELECT COUNT(*) FROM sensor_readings;")
        echo "Total readings persisted: $TOTAL"
        echo ""
        if [[ "$TOTAL" -gt 0 ]]; then
            SENSORS=$(psql_cmd "SELECT COUNT(DISTINCT sensor_id) FROM sensor_readings;")
            FIRST=$(psql_cmd "SELECT MIN(timestamp) FROM sensor_readings;")
            LAST=$(psql_cmd "SELECT MAX(timestamp) FROM sensor_readings;")
            echo "Distinct sensors: $SENSORS"
            echo "First reading:    $FIRST"
            echo "Latest reading:   $LAST"
            echo ""
            echo "--- Latest 10 readings ---"
            psql_pretty "SELECT id, sensor_id, temperature, humidity, location, kafka_partition, kafka_offset, timestamp FROM sensor_readings ORDER BY id DESC LIMIT 10;"
        else
            echo "No readings found. Is the pipeline running?"
            echo "  1. Start infra:   ./scripts/run.sh infra"
            echo "  2. Start apps:    ./gradlew :sensor-ingest:bootRun / :telemetry-processor:bootRun"
            echo "  3. Send data:     ./scripts/generate-data.sh --count 5"
        fi
        ;;

    --count|-c)
        check_connection
        TOTAL=$(psql_cmd "SELECT COUNT(*) FROM sensor_readings;")
        echo "$TOTAL"
        ;;

    --watch|-w)
        check_connection
        echo "Watching database for new records (Ctrl+C to stop)..."
        echo ""
        PREV=0
        while true; do
            TOTAL=$(psql_cmd "SELECT COUNT(*) FROM sensor_readings;")
            DIFF=$((TOTAL - PREV))
            if [[ $DIFF -gt 0 ]]; then
                TIMESTAMP=$(date +"%H:%M:%S")
                echo "[$TIMESTAMP] Total: $TOTAL (+$DIFF new)"
            fi
            PREV=$TOTAL
            sleep 2
        done
        ;;

    --sensors)
        check_connection
        echo "=== Per-Sensor Breakdown ==="
        echo ""
        psql_pretty "SELECT sensor_id, COUNT(*) as readings, ROUND(AVG(temperature)::numeric, 1) as avg_temp, ROUND(AVG(humidity)::numeric, 1) as avg_humidity, MAX(timestamp) as last_reading FROM sensor_readings GROUP BY sensor_id ORDER BY readings DESC;"
        ;;

    --alerts|-a)
        check_connection
        echo "=== Readings Above Alert Threshold (35°C) ==="
        echo ""
        psql_pretty "SELECT id, sensor_id, temperature, humidity, location, timestamp FROM sensor_readings WHERE temperature > 35.0 ORDER BY timestamp DESC LIMIT 20;"
        ALERT_COUNT=$(psql_cmd "SELECT COUNT(*) FROM sensor_readings WHERE temperature > 35.0;")
        echo "Total alert-level readings: $ALERT_COUNT"
        ;;

    *)
        echo "Usage: $0 {--summary|--count|--watch|--sensors|--alerts}"
        echo ""
        echo "Options:"
        echo "  --summary, -s   Total count + latest 10 readings (default)"
        echo "  --count, -c     Just print the total record count"
        echo "  --watch, -w     Poll every 2s showing new record counts"
        echo "  --sensors       Per-sensor breakdown with averages"
        echo "  --alerts, -a    Show readings that exceeded temperature threshold"
        exit 1
        ;;
esac
