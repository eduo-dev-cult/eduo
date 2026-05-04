# Setup guide local llm model.

## Requirements:

- [ ] Have Docker installed locally to run LLM within:
  [Docker Desktop | Docker Docs](https://docs.docker.com/desktop/)

There are also scripts provided which automate the process  of:

1. Starting the docker container. (which sets the port and everything for API to call from)

2. Starting Ollama (through which the LLM is provided and run)

3. Checks for conflicts (already running etc)

## About this guide:

- **Running scripts** part: show's you how to run the LLM inside a docker container based on the scripts already provided in the /scripts folder. 

- **manual setups** part is if you do not want to run the script then there is documentation here for manual setup.

## Running scripts (recommended)

The script does the following automated:

- Checks if Ollama is already running, otherwise starts it.
- Starts Docker container if needed
- Waits until the API is ready
- Pulls the required model (`gemma:2b`)
- Makes the docker container run the model in the backgrund,  awaiting query on:
  
  ```url
  http://localhost:11434
  ```
- Prints a test commando output

### Linux / macOS

Make sure the file is runnable:

```
chmod +x scripts/setup_llm.sh
```

Run the script:

```bash
./scripts/setup_llm.sh
```

#### Verify that it works:

```bash
curl -s http://localhost:11434/api/generate \
  -H "Content-Type: application/json" \
  -d '{"model":"gemma:2b","prompt":"Hello","stream":false}' \
| jq -r '.response'
```

Expected output  (might take a couple of seconds first time, loading model into RAM):
*A short response from the LLM model.*

### Windows (PowerShell)

```powershell
powershell -ExecutionPolicy Bypass -File scripts/setup.ps1
```

#### Verify that it works:

```powershell
$response = Invoke-RestMethod -Uri "http://localhost:11434/api/generate" `
  -Method Post `
  -ContentType "application/json" `
  -Body '{"model":"gemma:2b","prompt":"Hello","stream":false}'

$response.response
```

Expected output (might take a couple of seconds first time, loading model into RAM): *A short response from the LLM model.*

## Manual setup (no scripts)

This section shows how to run everything manually using the provided `docker-compose.yml`. 

This guide  is  for both operating systems, but if they deviate options are shown:

#### 1. Start the container

From the project root (where `docker-compose.yml` is located):  

```bash  ```
docker compose up -d
```

#### 2. Verify that it is running:

```bash
docker compose ps
```

Expected output: see Ollama status response with `running`

#### 3. Verify API is reachable

```bash
curl http://localhost:11434
```

#### 4. Pull the model

First you need to know what name docker gave to the container:

Find <container_name>:

```bash
docker ps
```

Then, input that container name in the following command:

```bash
docker exec -it <container_name> ollama pull gemma:2b
```

#### 5. Test the model and endpoint:

##### Linux / macOS

```bash
curl -s http://localhost:11434/api/generate \
  -H "Content-Type: application/json" \
  -d '{"model":"gemma:2b","prompt":"Hello","stream":false}' \
| jq -r '.response'
```

##### Windows (PowerShell)

```powershell
$response = Invoke-RestMethod -Uri "http://localhost:11434/api/generate" `
  -Method Post `
  -ContentType "application/json" `
  -Body '{"model":"gemma:2b","prompt":"Hello","stream":false}'

$response.response
```

##### Expected output (success test)

*A short response from the LLM model.*

## Stopping the container

```bash
docker compose down
```
