#!/usr/bin/env bash
set -e

MODEL="gemma:2b"
URL="http://localhost:11434"

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
  docker compose up -d

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
echo "Ollama running at: $URL"
echo "Model: $MODEL"
echo "Test with:"
echo "curl $URL/api/generate -d '{\"model\":\"$MODEL\",\"prompt\":\"Hello\",\"stream\":false}'"
echo "===================================="