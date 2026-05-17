# Eduo

Eduo is a full-stack application with a React frontend, Spring Boot backend, PostgreSQL database, and optional local LLM generation through Ollama.

# Tech stack

| Layer | Technology | Role |
|---|---|---|
| Frontend | React | UI |
| Backend | Spring Boot | REST API |
| Database | PostgreSQL | User data and saved work |
| AI | Ollama / local model | Task generation |

# Prerequisites

Install the following before running the project:

- Java 25 or compatible JDK
- Maven, or use the included Maven wrapper `./mvnw`
- Node.js and npm
- PostgreSQL
- Docker, only needed when running the local LLM service

# Build instructions
To build this project some setup is required. This needs to be done first.

Here is the quick start if setup is already done, otherwise it is mandatory to setup first (See [Setup summary](#setup-summary)):
First ensure PostgreSQL is running. If using the local LLM profile, also ensure the Docker LLM container is running.

In a separate terminal, start backend, use -e flag for more info during runtime:
```bash
mvn spring-boot:run
```
Then, In another separate terminal:
First move to where frontend is, from project root:
```bash
cd eduo-frontend
```
Then start it:
```bash
npm run dev
```

Then open the application as per:
NPM should tell you the URL endpoint for launching program in browser but default is:
```bash
http://localhost:5173/
```

## Setup summary:
- Set up postgresql (NOTE: remember all your details)
- Change `.env.example` (located at project root) name to `.env` and populate it as per your postgresql setup. 
- Setup the docker container as per 'infra/llm/README.md'
- Start backend
- Start frontend
- Open application in web-browser: http://localhost:5173/

### Setup postgresql AND .env file:

**UNIX commands: (otherwise do manually via GUI via PGADMIN)**

#### 1. Install PostgreSQL
1) install postgresql: 
https://www.postgresql.org/download/

2) Initialize the database directory if PostgreSQL has not already been initialized:
```bash
sudo postgresql-setup --initdb
```

3) Start and enable PostgreSQL:
```bash
sudo systemctl enable --now postgresql
```

4) Verify that it is running:
```bash
systemctl status postgresql
```

#### 2. Create the development database
1) Switch to the PostgreSQL admin user:
```bash
sudo -iu postgres
```

2) (Inside postgres terminal) Create a PostgreSQL role for your Linux user:
```bash
createuser --superuser {USER_NAME_HERE}
```

3) (Inside postgres terminal) Create the development database:
```bash
createdb -O {USER_NAME_HERE} eduo_dev_cult
```

4) Set a password for the PostgreSQL role:
```bash
psql
```
Inside psql:
```bash
ALTER USER {USER_NAME_HERE} WITH PASSWORD 'eduoPsw';
\q
```

5) Exit the postgres user:
```bash
exit
```

#### 3. Enable password authentication for local connections
Edit PostgreSQL’s authentication config:
```bash
sudo nano /var/lib/pgsql/data/pg_hba.conf
```
Change the local TCP connection methods from `ident` to `scram-sha-256`:
```conf
# IPv4 local connections:
host    all             all             127.0.0.1/32            scram-sha-256

# IPv6 local connections:
host    all             all             ::1/128                 scram-sha-256
```
Reload PostgreSQL:
```bash
sudo systemctl reload postgresql
```

#### 4. Test the connection
```bash
psql -U {USER_NAME_HERE} -d eduo_dev_cult -h localhost
```
Password:
```bash
eduoPsw
```
Inside psql, verify the active database and user:
```SQL
SELECT current_database(), current_user;
```
Exit with:
```SQL
\q
```
#### 5. Configure the project .env file
You can repurpose the .env.example file by renaming it .env and populating it. If you followed this guide you should only need to change the value following "DB_USER=" to the username you used instead of `{USER_NAME_HERE}`.

### Setup docker (local LLM)

This project uses Ollama for local LLM-based generation.

Ollama does not need to be installed directly on the host machine. It runs through the provided Docker setup.

Unix/Linux/macOS setup:

```bash
cd infra/llm
chmod +x scripts/setup_llm.sh
./scripts/setup_llm.sh
```
For non UNIX systems (Windows) use:

```powershell
cd infra/llm
powershell -ExecutionPolicy Bypass -File scripts/setup_llm.ps1
```

More details are available in `infra/llm/README.md`.

### Start backend
in a separate terminal, start backend, use -e flag for more info during runtime:
```bash
mvn spring-boot:run
```

### Start frontend
In another separate terminal:
First move to where frontend is, from project root:
```bash
cd eduo-frontend
```
If first time running, install frontend first:
```bash
npm install
```
Then start it:
```bash
npm run dev
```
### Open application
NPM should tell you the URL endpoint for launching program in browser but default is:
```
http://localhost:5173/
```

# API documentation
Swagger's interaktiva UI (tillgänglig när applikationen körs): http://localhost:8080/swagger-ui/index.html
Dokumentation i JSON-format: http://localhost:8080/v3/api-docs

# Branch organisation
- **Feature-branches** - Prefix "feature/", utveckling dedikerat till ett specifikt issue på projekttavlan
- **dev** - det alla feature branches baseras på, PR från feature branch med minst ett godkänt
- **main** - PR från dev, minst 2 godkänt, veckovisa pulls


