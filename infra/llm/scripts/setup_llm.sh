#!/usr/bin/env bash
set -e

# Resolve the script's own directory first so the rest of the paths work
# even when the script is started from somewhere else.
SCRIPT_DIR=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)

# The compose file lives one level above this script inside infra/llm.
LLM_DIR=$(cd -- "$SCRIPT_DIR/.." && pwd)

# Default to the Spring profile config that the app itself reads.
CONFIG_FILE_DEFAULT="$LLM_DIR/../../src/main/resources/application-ollama.properties"

# Always read from the same Spring profile config file as the app.
CONFIG_FILE="$CONFIG_FILE_DEFAULT"

read_property() {
  # key: property name to read, default_value: fallback if not found,
  # file_path: source properties file.
  local key="$1"
  local default_value="$2"
  local file_path="$3"
  local value=""

  if [[ -f "$file_path" ]]; then
    # Read the last matching key=value entry, strip trailing whitespace,
    # and remove CR characters in case the file was edited on Windows.
    value=$(grep -E "^[[:space:]]*${key}=" "$file_path" | tail -n 1 | cut -d= -f2- | sed 's/[[:space:]]*$//' | tr -d '\r')
  fi

  if [[ -n "$value" ]]; then
    # Return the configured value when the property exists.
    printf '%s' "$value"
  else
    # Fall back to a safe default so the script still works.
    printf '%s' "$default_value"
  fi
}

# Read model and base URL directly from application-ollama.properties.
MODEL="$(read_property "ollama.model" "gemma:2b" "$CONFIG_FILE")"
URL="$(read_property "ollama.base-url" "http://localhost:11434" "$CONFIG_FILE")"

echo "Checking if Ollama is already running on $URL..."

if curl -s "$URL" >/dev/null 2>&1; then
  echo "Ollama already running. Skipping Docker setup."
  USE_DOCKER=false
else
  USE_DOCKER=true
fi

# Only check Docker if we actually need it
if [ "$USE_DOCKER" = true ]; then
  echo "Checking Docker..."

  if ! command -v docker >/dev/null 2>&1; then
    echo "Docker is not installed."
    exit 1
  fi

  echo "Starting Ollama via Docker..."
  docker compose -f "$LLM_DIR/docker-compose.yml" up -d

  echo "Waiting for Ollama to be ready..."

  for i in {1..30}; do
    if curl -s "$URL" >/dev/null 2>&1; then
      echo "Ollama is up!"
      break
    fi
    echo "Waiting... ($i)"
    sleep 1
  done
fi

echo "Ensuring model exists: $MODEL"

if command -v ollama >/dev/null 2>&1 && [ "$USE_DOCKER" = false ]; then
  # Local Ollama install
  ollama pull "$MODEL"
else
  # Docker container
  CONTAINER=$(docker ps --filter "ancestor=ollama/ollama" --format "{{.Names}}" | head -n1)

  if [ -z "$CONTAINER" ]; then
    echo "Could not find running Ollama container."
    exit 1
  fi

  docker exec "$CONTAINER" ollama pull "$MODEL"
fi

echo ""
echo "===================================="
echo "Setup complete."
echo "Config file: $CONFIG_FILE"
echo "Ollama running at: $URL"
echo "Model: $MODEL"
echo "Test with:"
echo "curl $URL/api/generate -d '{\"model\":\"$MODEL\",\"prompt\":\"Hello\",\"stream\":false}'"
echo "===================================="