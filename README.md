# Build instructions

## Local LLM

This project uses Ollama for local LLM-based generation.

Setup instructions:

```bash
cd infra/llm
chmod +x setup_llm.sh
./scripts/setup_llm.sh
```
For non UNIX systems (Windows) use:

```powershell
cd infra/llm
powershell -ExecutionPolicy Bypass -File scripts/setup_llm.ps1
```

More details are available in `infra/llm/README.md`.

# Tech stack
| Layer | Technology | Role |
|---|---|---|
| Frontend | React | UI |
| Backend | Spring Boot | REST API |
| Database | PostgreSQL | User data, saved work? |
| AI | Local model (dev) | Task generation |

# Branch organisation
- **Feature-branches** - Prefix "feature/", utveckling dedikerat till ett specifikt issue på projekttavlan
- **dev** - det alla feature branches baseras på, PR från feature branch med minst ett godkänt
- **main** - PR från dev, minst 2 godkänt, veckovisa pulls


