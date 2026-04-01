# Log Aggregator Showcase

This is a **presentation demo project** for "Claude and the Mystery of Integrations." It showcases how to test third-party integrations using mock servers, failure scenario testing, and exponential backoff — all built with Claude Code assistance.

## What this project is

A microservice-based log management system that archives data to external NOARK-compliant archiving systems through an adapter pattern. The key point: the external systems (Noark A, Noark B) are on the customer side and we can't control them. We mock them for testing.

## Architecture

```
┌──────────────────┐
│  Angular Frontend │ :4200
└────────┬─────────┘
         │ HTTP
┌────────▼─────────┐
│   Log Manager     │ :8080  Spring Boot + MySQL (:3307)
└────────┬─────────┘
         │ RabbitMQ (:5672)
┌────────▼─────────┐
│      Edge         │ :8081  Spring Boot + MongoDB (:27017)
└───┬──────────┬───┘
    │ HTTP     │ HTTP
┌───▼───┐ ┌───▼───┐
│Adapt A│ │Adapt B│  :8082 / :8083  Spring Boot
└───┬───┘ └───┬───┘
    │ HTTP     │ HTTP
┌───▼──────────▼───┐
│ external-apis-mock│ :8084  Spring Boot (pretends to be Noark A & B)
└──────────────────┘
```

## Data flow

1. User creates a log group, adds string entries, closes the group (via Angular UI → Log Manager API)
2. Every mutation (create, add entry, close) publishes an event to RabbitMQ (exchange: `log-manager-exchange`, routing key: `log.event`)
3. Edge consumes events, tracks archive state in MongoDB, and pushes data to both Adapter A and Adapter B
4. Adapter A transforms data to JSON and POSTs to Noark A; Adapter B creates a ZIP and POSTs to Noark B
5. In dev/test, adapters point to `external-apis-mock` instead of real Noark systems

## Services

| Service | Port | Tech | What it does |
|---------|------|------|-------------|
| `frontend/` | 4200 | Angular + Tailwind | UI: log management (left), system overview panels (right) |
| `log-manager/` | 8080 | Spring Boot + MySQL | REST API for groups/entries, publishes to RabbitMQ |
| `edge/` | 8081 | Spring Boot + MongoDB | Consumes RabbitMQ, tracks archive state, routes to adapters, retries failures |
| `adapter-noark-a/` | 8082 | Spring Boot | Transforms to JSON, POSTs to Noark A |
| `adapter-noark-b/` | 8083 | Spring Boot | Transforms to ZIP, POSTs to Noark B |
| `external-apis-mock/` | 8084 | Spring Boot | Controllable mock of Noark A & B with `/api/test/setup`, `/api/test/reset`, `/api/test/history` |
| `integration-tests/` | — | Playwright (Node.js) | E2E tests with browser automation |

## Key APIs

### Log Manager (:8080)
```
POST   /api/groups              — create group { name }
GET    /api/groups              — list all groups
GET    /api/groups/{id}         — get group with entries
GET    /api/groups/{id}/entries — list entries
POST   /api/groups/{id}/entries — add entry { content }
POST   /api/groups/{id}/close   — close/archive group
```

### Edge (:8081)
```
GET    /api/status              — all archive states (alias: /api/archive-state)
GET    /api/groups              — all tracked groups
GET    /api/groups/{id}         — single group state
POST   /api/retry               — trigger retry of failed groups
```

### External APIs Mock (:8084)
```
POST   /api/test/setup          — configure mock responses (status codes, delays, error payloads)
POST   /api/test/reset          — reset to happy-path defaults
GET    /api/test/history        — log of all received requests
POST   /api/noarka/**           — mock Noark A endpoints
POST   /api/noarkb/**           — mock Noark B endpoints
```

### Adapters (:8082, :8083)
```
POST   /archive                 — receive archive request from Edge
```

## Running the project

```bash
./start-all.sh          # starts Docker infra + all services + frontend
# type 'stop' to shut down
```

Or start infrastructure only: `docker compose up -d`

## Testing and debugging

The `integration-tests/` folder has a full Playwright setup with Claude agents for test planning, generation, and healing. If you need to debug frontend issues or verify UI behavior, work from `integration-tests/` — it has browser automation capabilities and its own CLAUDE.md with details on running tests and using Playwright agents.

```bash
cd integration-tests
npx playwright test --trace on            # run all tests
npx playwright test tests/specific.spec.ts # run one test
```

## Presentation demo flow

This project exists for a live demo with three acts:
1. **Happy path** — run the e2e test showing the full create → archive flow working
2. **Error handling** — configure the mock to return 500s, show Edge captures failures in MongoDB
3. **Live coding** — implement exponential backoff with Claude Code, run the backoff test to prove it works

## Important context for Claude Code agents

- All Spring Boot services use Java 17, Spring Boot 3.5.x, Lombok
- RabbitMQ credentials: user `myuser`, pass `secret`
- RabbitMQ Management UI: http://localhost:15672
- MySQL credentials: user `myuser`, pass `secret`, root pass `verysecret`
- MongoDB credentials: user `root`, pass `secret`
- The frontend uses `data-testid` attributes throughout — use these for Playwright selectors
- Check `PLAN.md` for current implementation status and what's left to do
- Check `PRD.md` for full product requirements
