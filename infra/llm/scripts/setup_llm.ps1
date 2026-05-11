# abort all changes if error (safer)
$ErrorActionPreference = "Stop"

# Resolve the infra/llm directory from the script location so the rest of the
# paths still work when the script is started from another folder.
$LlmDir = Split-Path -Parent $PSScriptRoot

# Default to the same Spring profile config file the application reads.
$DefaultConfigFile = Join-Path $LlmDir "..\..\src\main\resources\application-ollama.properties"

# Always read from the same Spring profile config file as the application.
$ConfigFile = $DefaultConfigFile

function Get-PropertyValue {
    param(
        # FilePath: properties file to read from.
        [string]$FilePath,

        # Key: property name to look up.
        [string]$Key,

        # DefaultValue: fallback when the property is missing.
        [string]$DefaultValue
    )

    if (Test-Path $FilePath) {
        # Read the last matching key=value entry so later definitions win.
        $match = Get-Content $FilePath |
            Where-Object { $_ -match "^\s*$([regex]::Escape($Key))=(.+)$" } |
            Select-Object -Last 1

        if ($match) {
            # Remove the "key=" prefix and trim surrounding whitespace.
            return ($match -replace "^\s*$([regex]::Escape($Key))=", "").Trim()
        }
    }

    # Fall back to a safe default so the script still works without the file.
    return $DefaultValue
}

# Read model and base URL directly from application-ollama.properties.
$MODEL = Get-PropertyValue -FilePath $ConfigFile -Key "ollama.model" -DefaultValue "gemma:2b"
$URL = Get-PropertyValue -FilePath $ConfigFile -Key "ollama.base-url" -DefaultValue "http://localhost:11434"

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
        $install = Read-Host "Docker is not installed. Install Docker Desktop via winget? [y/N]"
        if ($install -eq 'y' -or $install -eq 'Y') {
            Write-Host "Installing Docker Desktop via winget..."
            winget install --id Docker.DockerDesktop -e --accept-source-agreements --accept-package-agreements
            Write-Host "Docker Desktop installed. Please start it, then re-run this script."
            exit 0
        }
    }

    Write-Host "Starting Ollama via Docker..."
    docker compose -f (Join-Path $LlmDir "docker-compose.yml") up -d

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
Write-Host "Config file: $ConfigFile"
Write-Host "Ollama running at: $URL"
Write-Host "Model: $MODEL"
Write-Host "Test with:"
Write-Host "curl $URL/api/generate -d '{\"model\":\"$MODEL\",\"prompt\":\"Hello\",\"stream\":false}'"
Write-Host "===================================="