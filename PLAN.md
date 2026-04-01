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

### Integration tests — STARTED
- [x] Playwright project set up with config
- [x] `log-group-lifecycle.spec.ts` — full e2e test: create group → add 3 entries → close → verify UI state + RabbitMQ message count
- [ ] `seed.spec.ts` — empty scaffold placeholder

## What's left to do

### Implementation (per PRD)
- [ ] **F3: Edge** — RabbitMQ consumer, MongoDB state, push to adapters, error recording
- [ ] **F4: Adapter Noark A** — receive from Edge, transform to JSON, POST to Noark A
- [ ] **F5: Adapter Noark B** — receive from Edge, transform to ZIP, POST to Noark B
- [ ] **F6: external-apis-mock** — mock Noark A/B endpoints, test setup/reset/history
- [ ] **Integration tests** — error handling test, exponential backoff test

### Claude Code infra
- [ ] Root `CLAUDE.md` — architecture, service map, tech stack, port map
- [ ] Per-service `CLAUDE.md` — API contracts, DB schemas, dependencies
- [ ] Skills for the live demo (add retry logic, create integration test, setup mock behavior)
- [ ] Skill that feeds NOARK Swagger specs as context

### Presentation demo
1. Run happy path test — show green results
2. Run error test — show failure captured in Edge MongoDB
3. Live-code exponential backoff with Claude Code, run backoff test to prove it works

## Out of scope

- Auth
- Production deployment
- Real NOARK connectivity
- Non-string log entries
- Multi-tenancy
- Load testing
