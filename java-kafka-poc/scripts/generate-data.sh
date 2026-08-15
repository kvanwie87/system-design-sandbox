#!/usr/bin/env bash
#
# Sends randomized sensor readings to the sensor-ingest service (macOS/Linux).
#
# Usage:
#   ./scripts/generate-data.sh                           # Default: 5 sensors, 1s interval, infinite
#   ./scripts/generate-data.sh --count 50 --interval 500
#   ./scripts/generate-data.sh --sensors 10 --interval 200
#
# Options:
#   --url <base_url>      Base URL (default: http://localhost:8080)
#   --interval <ms>       Milliseconds between requests (default: 1000)
#   --count <n>           Number of readings, 0=infinite (default: 0)
#   --sensors <n>         Number of simulated sensors (default: 5)

set -euo pipefail

BASE_URL="http://localhost:8080"
INTERVAL_MS=1000
COUNT=0
NUM_SENSORS=5

while [[ $# -gt 0 ]]; do
    case "$1" in
        --url)      BASE_URL="$2"; shift 2 ;;
        --interval) INTERVAL_MS="$2"; shift 2 ;;
        --count)    COUNT="$2"; shift 2 ;;
        --sensors)  NUM_SENSORS="$2"; shift 2 ;;
        *)          echo "Unknown option: $1"; exit 1 ;;
    esac
done

LOCATIONS=("warehouse-A" "warehouse-B" "server-room" "rooftop" "basement" "lab-1" "lab-2" "cold-storage" "furnace-room" "outdoor-north")

random_range() {
    local min=$1 max=$2
    echo $(( RANDOM % (max - min + 1) + min ))
}

random_choice() {
    local arr=("$@")
    echo "${arr[RANDOM % ${#arr[@]}]}"
}

# Generate a decimal from integer range (avoids bc dependency)
# Usage: random_decimal min_x10 max_x10 -> outputs value with 1 decimal place
random_decimal() {
    local min_x10=$1 max_x10=$2
    local val=$(( RANDOM % (max_x10 - min_x10 + 1) + min_x10 ))
    local whole=$(( val / 10 ))
    local frac=$(( val % 10 ))
    echo "${whole}.${frac}"
}

generate_reading() {
    local sensor_num
    sensor_num=$(random_range 1 "$NUM_SENSORS")
    local sensor_id
    sensor_id=$(printf "sensor-%03d" "$sensor_num")
    local location
    location=$(random_choice "${LOCATIONS[@]}")

    # Temperature: mostly 15-30, 15% chance of spike above 35 (triggers alerts)
    local temp
    if [[ $(random_range 1 100) -le 15 ]]; then
        temp=$(random_decimal 350 550)
    else
        temp=$(random_decimal 150 300)
    fi

    local humidity
    humidity=$(random_decimal 200 900)

    # 40% chance of including battery level (v2 schema field)
    local battery_field=""
    if [[ $(random_range 1 100) -le 40 ]]; then
        local battery
        battery=$(random_decimal 100 1000)
        battery_field=", \"batteryLevel\": $battery"
    fi

    echo "{\"sensorId\": \"$sensor_id\", \"temperature\": $temp, \"humidity\": $humidity, \"location\": \"$location\"$battery_field}"
}

echo "Sensor Data Generator"
echo "  Target:   $BASE_URL/api/sensors/readings"
echo "  Sensors:  $NUM_SENSORS"
echo "  Interval: ${INTERVAL_MS}ms"
if [[ $COUNT -eq 0 ]]; then
    echo "  Count:    infinite (Ctrl+C to stop)"
else
    echo "  Count:    $COUNT"
fi
echo ""

SENT=0
ERRORS=0

# Convert interval to seconds for sleep (integer math, ms -> s.ms)
SLEEP_SEC=$(( INTERVAL_MS / 1000 ))
SLEEP_FRAC=$(( INTERVAL_MS % 1000 ))
if [[ $SLEEP_FRAC -gt 0 ]]; then
    # Pad to 3 digits
    SLEEP_ARG="${SLEEP_SEC}.$(printf '%03d' $SLEEP_FRAC)"
else
    SLEEP_ARG="$SLEEP_SEC"
fi

cleanup() {
    echo ""
    echo "Summary: $SENT sent, $ERRORS errors"
    exit 0
}
trap cleanup SIGINT SIGTERM

while true; do
    JSON=$(generate_reading)

    HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" \
        -X POST "$BASE_URL/api/sensors/readings" \
        -H "Content-Type: application/json" \
        -d "$JSON" 2>/dev/null || echo "000")

    SENT=$((SENT + 1))

    # Parse sensor ID and temp from the JSON for display
    SENSOR_ID=$(echo "$JSON" | sed -n 's/.*"sensorId": *"\([^"]*\)".*/\1/p')
    TEMP=$(echo "$JSON" | sed -n 's/.*"temperature": *\([0-9.]*\).*/\1/p')
    HUMIDITY=$(echo "$JSON" | sed -n 's/.*"humidity": *\([0-9.]*\).*/\1/p')
    LOCATION=$(echo "$JSON" | sed -n 's/.*"location": *"\([^"]*\)".*/\1/p')

    if [[ "$HTTP_CODE" == "202" || "$HTTP_CODE" == "200" ]]; then
        ALERT=""
        COLOR="\033[32m"  # green
        # Compare temp > 35 using integer math (temp is X.Y so multiply by 10)
        TEMP_INT=$(echo "$TEMP" | tr -d '.')
        if [[ ${#TEMP_INT} -eq 2 ]]; then TEMP_INT="${TEMP_INT}0"; fi
        if [[ $TEMP_INT -gt 350 ]]; then
            ALERT=" [ALERT]"
            COLOR="\033[31m"  # red
        fi
        echo -e "${COLOR}[$SENT] $SENSOR_ID | temp=${TEMP}C | humidity=${HUMIDITY}% | $LOCATION$ALERT\033[0m"
    else
        ERRORS=$((ERRORS + 1))
        echo -e "\033[31m[$SENT] ERROR: HTTP $HTTP_CODE\033[0m"
    fi

    if [[ $COUNT -gt 0 && $SENT -ge $COUNT ]]; then break; fi
    sleep "$SLEEP_ARG"
done

cleanup
