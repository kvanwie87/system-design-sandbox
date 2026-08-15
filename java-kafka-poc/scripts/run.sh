#!/usr/bin/env bash
#
# Manages the Kafka PoC Docker Compose stack (macOS/Linux).
#
# Usage:
#   ./scripts/run.sh up        # Full stack: infra + apps
#   ./scripts/run.sh infra     # Infrastructure only
#   ./scripts/run.sh down      # Tear everything down
#   ./scripts/run.sh build     # Build JARs then Docker images
#   ./scripts/run.sh logs      # Tail logs
#   ./scripts/run.sh status    # Show container status

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
cd "$PROJECT_ROOT"

COMMAND="${1:-up}"

case "$COMMAND" in
    up)
        echo "Starting full stack (infra + apps)..."
        docker compose --profile app up -d --build
        ;;
    infra)
        echo "Starting infrastructure only (Kafka, Schema Registry, PostgreSQL)..."
        docker compose up -d
        echo ""
        echo "Infrastructure is up. Run apps locally with:"
        echo "  ./gradlew :sensor-ingest:bootRun"
        echo "  ./gradlew :telemetry-processor:bootRun"
        ;;
    down)
        echo "Stopping all services..."
        docker compose --profile app down -v
        ;;
    build)
        echo "Building application JARs..."
        ./gradlew :sensor-ingest:bootJar :telemetry-processor:bootJar --no-daemon --console=plain
        echo "Building Docker images..."
        docker compose --profile app build
        echo "Done. Run './scripts/run.sh up' to start."
        ;;
    logs)
        docker compose --profile app logs -f
        ;;
    status)
        docker compose --profile app ps
        ;;
    *)
        echo "Usage: $0 {up|infra|down|build|logs|status}"
        echo ""
        echo "Commands:"
        echo "  up      Start full stack (infra + apps)"
        echo "  infra   Start infrastructure only (run apps locally)"
        echo "  down    Stop and remove all services"
        echo "  build   Build JARs and Docker images"
        echo "  logs    Tail logs for all services"
        echo "  status  Show running containers"
        exit 1
        ;;
esac
