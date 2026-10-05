#!/usr/bin/env bash
# Shared helpers sourced by the other scripts.

# Resolve the project root (the parent of this scripts/ directory).
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

# Base URL the app serves on.
BASE_URL="${BASE_URL:-http://localhost:8080}"

# Credentials for the form-login / basic user. These match the
# spring.security.user.* values in application.yaml and can be overridden
# via environment variables.
APP_USER="${APP_USER:-user}"
APP_PASSWORD="${APP_PASSWORD:-password}"

# Ensure a usable JAVA_HOME. The project's default JAVA_HOME (sdkman "current"
# symlink) may be broken, so fall back to a known-good JDK 21 if needed.
ensure_java_home() {
	if [ -n "${JAVA_HOME:-}" ] && [ -x "${JAVA_HOME}/bin/java" ]; then
		return 0
	fi

	local candidates=(
		"/c/sdkman/candidates/java/21.0.9-amzn"
		"/c/Program Files/Amazon Corretto/jdk21.0.4_7"
	)
	for c in "${candidates[@]}"; do
		if [ -x "${c}/bin/java" ]; then
			export JAVA_HOME="${c}"
			echo "Using JAVA_HOME=${JAVA_HOME}"
			return 0
		fi
	done

	echo "WARNING: Could not find a valid JDK 21. Set JAVA_HOME manually." >&2
}
