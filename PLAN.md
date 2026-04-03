# Plan

Working doc for coordinating implementation of the Log Aggregator Showcase.

## Project structure

```
log-aggregator-showcase/
├── frontend/                 # Angular + Tailwind CSS
├── log-manager/              # Spring Boot 3
├── edge/                     # Spring Boot 3 + MongoDB
├── adapter-noark-a/          # Spring Boot 3
├── adapter-noark-b/          # Spring Boot 3
├── external-apis-mock/       # Spring Boot 3
├── integration-tests/        # Playwright E2E tests (Node.js)
├── docker-compose.yml        # Infrastructure (MySQL, MongoDB, RabbitMQ)
├── start-all.sh              # Starts infra + all services
├── CLAUDE.md
├── PRD.md
├── PLAN.md
└── .claude/
    └── skills/
```

## Port map

| Service              | Port  |
|----------------------|-------|
| Frontend (Angular)   | 4200  |
| Log Manager          | 8080  |
| Edge                 | 8081  |
| Adapter Noark A      | 8082  |
| Adapter Noark B      | 8083  |
| External APIs Mock   | 8084  |
| MySQL                | 3307  |
| MongoDB              | 27017 |
| RabbitMQ (AMQP)      | 5672  |
| RabbitMQ (Management)| 15672 |

## Tech stack

- All backend services: Spring Boot 3.5.x + Java 17
- Frontend: Angular + Tailwind CSS
- integration-tests: Playwright (Node.js)
- Log Manager DB: MySQL
- Edge DB: MongoDB
- Messaging: RabbitMQ
- Infra: Docker Compose
- Hot-reload: Spring Boot DevTools + IntelliJ auto-build

## What's done

### Infrastructure
- [x] PRD written (PRD.md)
- [x] Root docker-compose.yml for infrastructure (MySQL 3307, MongoDB 27017, RabbitMQ 5672/15672)
- [x] start-all.sh script to launch everything (with cleanup/stop support)
- [x] All 5 Spring Boot services scaffolded, packages renamed, dependencies trimmed

### Log Manager backend (F2) — COMPLETE
- [x] `LogGroupController` — 6 endpoints: create, list, get, get entries, add entry, close group
- [x] JPA entities: `LogGroup` (id, name, status, createdAt, updatedAt), `LogEntry` (id, content, createdAt, group FK)
- [x] Repositories: `LogGroupRepository`, `LogEntryRepository` with custom queries
- [x] `LogManagerService` — business logic with validation (can't add to closed group, can't close twice)
- [x] RabbitMQ integration: `RabbitMQConfig` (topic exchange "log-manager-exchange", queue "log-events-queue", routing key "log.event"), `RabbitMQPublisher` publishing GROUP_CREATED, ENTRY_ADDED, GROUP_CLOSED events
- [x] Unit tests: `LogGroupControllerTest` (5 tests), `LogManagerServiceTest` (6 tests)

### Frontend (F1) — COMPLETE
- [x] Split-screen layout: Log Manager on left, System Overview on right
- [x] Left panel: create group, select group, add entries, close group, view entries
- [x] Right panel: 3 polling panels (RabbitMQ, Edge/MongoDB, External APIs Mock) — poll every 3s
- [x] Angular services: `LogManagerApiService`, `RabbitmqPanelService`, `EdgePanelService`, `MockPanelService`
- [x] Models: `LogGroup`, `LogEntry` interfaces
- [x] Reactive forms, data-testid selectors throughout
- [x] Tailwind CSS styling

### Edge (F3) — COMPLETE
- [x] `RabbitConfig` — binds to exchange `log-manager-exchange`, queue `log-events-queue`, routing key `log.event`
- [x] `LogEventListener` — consumes GROUP_CREATED, ENTRY_ADDED, GROUP_CLOSED events from RabbitMQ
- [x] `ArchiveGroup` MongoDB document — groupId, name, status (PENDING/IN_PROGRESS/ARCHIVED/FAILED), entries, errors, retryCount
- [x] `ArchiveGroupRepository` — findByGroupId, findByStatus
- [x] `ArchiveService` — sends archive requests to both adapters, records errors, retry up to 3 attempts
- [x] `AdapterClient` — HTTP client for calling Adapter A and B
- [x] `StatusController` — GET /api/status (alias /api/archive-state), GET /api/groups, GET /api/groups/{id}, POST /api/retry
- [x] `RetryScheduler` — scheduled retry every 60s for failed groups
- [x] `LogEvent` model with `@JsonAlias` to handle both PLAN format and log-manager's actual field names (eventType→event, groupName→name, entryContent→content)
- [x] Unit tests: `ArchiveServiceTest` (5 tests), `LogEventListenerTest` (6 tests), `StatusControllerTest` (5 tests)

### Adapter Noark A (F4) — COMPLETE
- [x] `ArchiveController` — POST /archive, returns 200 on success, 502 on failure
- [x] `ArchiveService` — orchestrates transform + POST
- [x] `TransformService` — converts ArchiveRequest to NoarkAPayload (JSON with title, description, documents)
- [x] `NoarkAClient` — POSTs JSON payload to configurable Noark A endpoint (http://localhost:8084/api/noarka)
- [x] Unit tests: `TransformServiceTest` (3 tests), `ArchiveServiceTest` (3 tests), `ArchiveControllerTest` (2 tests)

### Adapter Noark B (F5) — COMPLETE
- [x] `ArchiveController` — POST /archive, returns 200 on success, 502 on failure
- [x] `ArchiveService` — orchestrates ZIP creation + POST
- [x] `ZipService` — creates ZIP file with metadata.json + entry-N.txt files
- [x] `NoarkBClient` — POSTs ZIP binary to configurable Noark B endpoint (http://localhost:8084/api/noarkb)
- [x] Unit tests: `ZipServiceTest` (4 tests), `ArchiveServiceTest` (3 tests), `ArchiveControllerTest` (2 tests)

### External APIs Mock (F6) — COMPLETE
- [x] `NoarkAController` — catch-all POST/GET/PUT on /api/noarka/**, returns configurable responses
- [x] `NoarkBController` — catch-all POST/GET/PUT on /api/noarkb/**, returns configurable responses
- [x] `TestController` — POST /api/test/setup (configure status/body/delay per endpoint), POST /api/test/reset, GET /api/test/history
- [x] `MockService` — in-memory config and request history tracking, thread-safe
- [x] Configurable response delays for timeout testing
- [x] Unit tests: `MockServiceTest` (6 tests), `NoarkAControllerTest` (4 tests), `TestControllerTest` (3 tests)

### Integration tests — STARTED
- [x] Playwright project set up with config
- [x] `log-group-lifecycle.spec.ts` — full e2e test: create group → add 3 entries → close → verify UI state + RabbitMQ message count
- [ ] `seed.spec.ts` — empty scaffold placeholder

## What's left to do

### Completed GitHub issues
- [x] **Issue #2** — Upgrade right-side panels: WebSocket RabbitMQ view, detailed Edge panel with retry, mock setup controls, global RESET button
  - [x] #5 — UI Design: overall layout and panel redesign
  - [x] #6 — Docker Compose: enable RabbitMQ Web STOMP plugin
  - [x] #7 — Edge panel: detailed archive state with status badges
  - [x] #8 — Mock panel: split Noark A/B history + setup controls
  - [x] #9 — RabbitMQ panel: WebSocket live view via STOMP
  - [x] #10 — Global RESET button
  - [x] #11 — Documentation updates (SpringDoc OpenAPI + Swagger UI + backwards compatibility tests)

### Integration tests
- [ ] Error handling test — configure mock to 500 → close group → verify FAILED in Edge
- [ ] Exponential backoff test — mock fails twice then succeeds → verify retries with increasing delay (this test is run on stage after live-coding backoff)

### API Documentation — COMPLETE
- [x] SpringDoc OpenAPI (Swagger UI) on all 5 services
- [x] Frontend `/docs` page with tabbed Swagger UI iframes
- [x] OpenAPI backwards compatibility tests using openapi-diff on all 5 services
- [x] CLAUDE.md updated to point to live Swagger UI instead of manual endpoint lists

### Claude Code infra
- [x] Root `CLAUDE.md` — architecture, service map, tech stack, port map, demo flow, sub-project guidance
- [x] `integration-tests/CLAUDE.md` — Playwright setup, agents, mock API usage
- [x] `frontend/.claude/CLAUDE.md` — Angular/TypeScript best practices
- [ ] Per-service `CLAUDE.md` for Spring Boot services (log-manager, edge, adapters, mock)
- [ ] Skills for the live demo (add retry logic, create integration test, setup mock behavior)
- [ ] Skill that feeds NOARK Swagger specs as context

### Presentation demo
1. Run happy path test — show green results
2. Run error test — show failure captured in Edge MongoDB
3. Live-code exponential backoff with Claude Code, run backoff test to prove it works

## Agent workflow notes

- **All agents start from the repo root.** They load root CLAUDE.md automatically.
- **Playwright MCP + browser agents** are only available when started from `integration-tests/`. Root agents can write test files and run `cd integration-tests && npx playwright test`, but cannot do interactive browser debugging.
- **To debug frontend in a browser**, start a separate agent from `integration-tests/` — it has the playwright-test MCP server and 3 specialized agents (planner, generator, healer).
- **Angular coding standards** are in `frontend/.claude/CLAUDE.md` — root agents should read this before modifying Angular code.

## Out of scope

- Auth
- Production deployment
- Real NOARK connectivity
- Non-string log entries
- Multi-tenancy
- Load testing
