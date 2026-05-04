$ErrorActionPreference = "Stop"

$MODEL = "gemma:2b"
$URL = "http://localhost:11434"

Write-Host "Checking if Ollama is already running on $URL..."

try {
    $response = Invoke-WebRequest -Uri $URL -UseBasicParsing -TimeoutSec 2
    Write-Host "Ollama already running. Skipping Docker setup."
    $USE_DOCKER = $false
} catch {
    $USE_DOCKER = $true
}

if ($USE_DOCKER) {
    Write-Host "Checking Docker..."

    if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
        Write-Error "Docker is not installed."
        exit 1
    }

    Write-Host "Starting Ollama via Docker..."
    docker compose up -d

    Write-Host "Waiting for Ollama to be ready..."

    for ($i = 1; $i -le 30; $i++) {
        try {
            Invoke-WebRequest -Uri $URL -UseBasicParsing -TimeoutSec 2 | Out-Null
            Write-Host "Ollama is up!"
            break
        } catch {
            Write-Host "Waiting... ($i)"
            Start-Sleep -Seconds 1
        }
    }
}

Write-Host "Ensuring model exists: $MODEL"

# Check if local Ollama CLI exists
$ollamaExists = Get-Command ollama -ErrorAction SilentlyContinue

if ($ollamaExists -and -not $USE_DOCKER) {
    ollama pull $MODEL
} else {
    $container = docker ps --filter "ancestor=ollama/ollama" --format "{{.Names}}" | Select-Object -First 1

    if (-not $container) {
        Write-Error "Could not find running Ollama container."
        exit 1
    }

    docker exec $container ollama pull $MODEL
}

Write-Host ""
Write-Host "===================================="
Write-Host "Setup complete."
Write-Host "Ollama running at: $URL"
Write-Host "Model: $MODEL"
Write-Host "Test with:"
Write-Host "curl $URL/api/generate -d '{\"model\":\"$MODEL\",\"prompt\":\"Hello\",\"stream\":false}'"
Write-Host "===================================="