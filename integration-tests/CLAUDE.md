# Log Aggregator — Integration Tests

Playwright test suite for the Log Aggregator Showcase application.

## What this tests

A microservice system that creates, manages, and archives logs to external NOARK-compliant systems. The frontend is Angular, backend is Spring Boot 3 microservices communicating via RabbitMQ.

## Services under test

| Service              | URL                    |
|----------------------|------------------------|
| Frontend (Angular)   | http://localhost:4200   |
| Log Manager API      | http://localhost:8080   |
| Edge (buffer/router) | http://localhost:8081   |
| Adapter Noark A      | http://localhost:8082   |
| Adapter Noark B      | http://localhost:8083   |
| External APIs Mock   | http://localhost:8084   |

Infrastructure: MySQL (:3307), MongoDB (:27017), RabbitMQ (:5672, mgmt :15672).

## Running tests

```bash
# Run all tests with trace
npx playwright test --trace on

# Run a specific test
npx playwright test tests/my-test.spec.ts --trace on

# Chromium only (default project)
npx playwright test --project=chromium --trace on
```

## Playwright agents

Three agents are available in `.claude/agents/`:

| Agent | When to use |
|-------|-------------|
| `playwright-test-planner` | Explore the app UI and produce a test plan in `specs/` |
| `playwright-test-generator` | Convert a test plan into executable Playwright test files |
| `playwright-test-healer` | Debug and fix failing tests automatically |

## Browser automation

Use the `playwright-cli` skill for interactive browser exploration. Run `playwright-cli open http://localhost:4200` to start.

## Key APIs for test setup

The **external-apis-mock** service (port 8084) supports runtime configuration:

- `POST /api/test/setup` — configure mock behavior (status codes, delays, error payloads)
- `POST /api/test/reset` — reset to default happy-path behavior
- `GET /api/test/history` — get log of all received requests (for assertions)

Use these in test `beforeEach`/`afterEach` to control external system behavior.
