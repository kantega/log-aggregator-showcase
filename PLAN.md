# Plan

Working doc for coordinating implementation of the Log Aggregator Showcase.

## Project structure

```
log-aggregator-showcase/
├── frontend/                 # Angular
├── log-manager/              # Spring Boot 3
├── edge/                     # Spring Boot 3 + MongoDB
├── adapter-noark-a/          # Spring Boot 3
├── adapter-noark-b/          # Spring Boot 3
├── external-apis-mock/       # Spring Boot 3
├── integration-tests/        # Node.js (pure)
├── CLAUDE.md
├── PRD.md
├── PLAN.md
└── .claude/
    └── skills/
```

## Tech stack notes

- All backend services: Spring Boot 3 + Java
- Frontend: Angular
- integration-tests: pure Node.js (no Spring)
- Log Manager DB: MySQL
- Edge DB: MongoDB
- Messaging: RabbitMQ
- Infra: Docker Compose

## Presentation demo

1. Run happy path test — show green results
2. Run error test — show failure captured in Edge MongoDB
3. Live-code exponential backoff with Claude Code, run backoff test to prove it works

## Integration tests

- **Happy path:** create group → add entries → close → verify archived in both mocks
- **Error:** configure mock to return 500 → trigger archiving → verify Edge records failure, no data loss
- **Exponential backoff:** mock fails twice then succeeds → verify retries with increasing delay → eventual success

## Claude Code infra we need

- Root `CLAUDE.md` — architecture, service map, tech stack
- Per-service `CLAUDE.md` — API contracts, DB schemas, dependencies
- Skills for the live demo (add retry logic, create integration test, setup mock behavior)
- Skill that feeds NOARK Swagger specs as context

## Out of scope

- Auth
- Production deployment
- Real NOARK connectivity
- Non-string log entries
- Multi-tenancy
- Load testing
