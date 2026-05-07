
# API documentation
Swagger's interaktiva UI (tillgänglig när applikationen körs): http://localhost:8080/swagger-ui/index.html
Dokumentation i JSON-format: http://localhost:8080/v3/api-docs

# Build instructions

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
