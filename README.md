# Log Aggregator Showcase: Claude & The Mystery of Integrations

This repository contains the companion code for the presentation **"Claude and the Mystery of Integrations."** It features a sample log aggregator application used to demonstrate how to effectively test third-party integrations, moving beyond standard "happy paths" to handle unpredictable real-world failures.

**Key demonstrations in this repo include:**
* **AI-Generated Mock Servers:** Using Claude to spin up a test server that accurately mimics an external provider.
* **Failure Scenario Testing:** Triggering custom responses to test how the application handles unreliability.
* **Testing Exponential Backoff:** Verifying that the system correctly succeeds on a second retry after an initial failure.
* **Claude Skills Integration:** Utilizing custom Skills to feed architectural context and test strategies into Claude, minimizing manual prompt engineering.

## Architecture

```
┌──────────────────┐
│  Angular Frontend │ :4200
└────────┬─────────┘
         │
┌────────▼─────────┐
│   Log Manager     │ :8080  (Spring Boot + MySQL)
└────────┬─────────┘
         │ RabbitMQ (:5672)
┌────────▼─────────┐
│      Edge         │ :8081  (Spring Boot + MongoDB)
└───┬──────────┬───┘
    │          │
┌───▼───┐ ┌───▼───┐
│Adapt A│ │Adapt B│  :8082 / :8083
└───┬───┘ └───┬───┘
    │          │
┌───▼──────────▼───┐
│ external-apis-mock│ :8084
└──────────────────┘
```

| Service | Port | Description |
|---------|------|-------------|
| Frontend | 4200 | Angular UI — log management + system overview |
| Log Manager | 8080 | REST API, MySQL storage, RabbitMQ publisher |
| Edge | 8081 | RabbitMQ consumer, MongoDB state, routes to adapters |
| Adapter Noark A | 8082 | Transforms and POSTs JSON to Noark A |
| Adapter Noark B | 8083 | Transforms and POSTs ZIP to Noark B |
| external-apis-mock | 8084 | Controllable mock of Noark A & B APIs |
| MySQL | 3307 | Log Manager database |
| MongoDB | 27017 | Edge state database |
| RabbitMQ | 5672 / 15672 | Message broker / Management UI |

## Prerequisites

- **Java 17**
- **Maven**
- **Node.js** (LTS)
- **Docker** and **Docker Compose**

## Getting started

### 1. Start everything

```bash
./start-all.sh
```

This will:
- Start infrastructure (MySQL, MongoDB, RabbitMQ) via Docker Compose
- Start all 5 Spring Boot services
- Start the Angular frontend

Type `stop` in the terminal to shut everything down.

### 2. Start infrastructure only

If you prefer to run services individually (e.g. from IntelliJ):

```bash
docker compose up -d
```

Then start each service manually with `mvn spring-boot:run` in its folder, or run it from your IDE.

### 3. Access the app

- **Frontend:** http://localhost:4200
- **RabbitMQ Management:** http://localhost:15672 (user: `myuser`, pass: `secret`)

## Project structure

```
log-aggregator-showcase/
├── frontend/                 # Angular + Tailwind CSS
├── log-manager/              # Spring Boot 3 — REST API + MySQL + RabbitMQ
├── edge/                     # Spring Boot 3 — MongoDB + RabbitMQ consumer
├── adapter-noark-a/          # Spring Boot 3 — JSON adapter
├── adapter-noark-b/          # Spring Boot 3 — ZIP adapter
├── external-apis-mock/       # Spring Boot 3 — controllable mock server
├── integration-tests/        # Playwright E2E tests
├── adapter-archetype/        # Maven archetype used by /add-mock-provider to scaffold new adapters
├── docker-compose.yml        # MySQL, MongoDB, RabbitMQ
└── start-all.sh              # Start/stop everything
```

## Adding a new provider

The `/add-mock-provider` Claude Code skill onboards a new Noark-compliant archive provider end-to-end. Given an OpenAPI spec, it:

1. Scaffolds a new adapter service (`adapter-<slug>/`) via `adapter-archetype`
2. Fills in the provider-specific payload, transform, and client endpoint from the spec
3. Adds a controller to `external-apis-mock/` for the new provider
4. Wires the new adapter into `edge/`, root `pom.xml`, and `start-all.sh`

### Prerequisite

Install the archetype to your local Maven repo once. Either run a full reactor build from the root:

```bash
mvn install -DskipTests
```

…or install just the archetype:

```bash
mvn install -pl adapter-archetype -DskipTests
```

### Run the skill

In Claude Code, from the repo root:

```
/add-mock-provider use specs/noark-c.yaml
```

The skill does not run tests or builds — after it finishes, run `mvn install -DskipTests` and the unit tests yourself.

## Running tests

### Unit tests (per service)

```bash
cd log-manager && mvn test
cd edge && mvn test
# etc.
```

### Integration tests (Playwright)

Make sure all services are running, then:

```bash
cd integration-tests
npm install
npx playwright test
```

The latest Playwright test results are published at https://kantega.github.io/log-aggregator-showcase/.

## Demo prompts

The presentation features three prompts. Prompts 1 and 2 were run against the codebase ahead of time and merged into `main`; on stage we show the prompt, then walk through the resulting code and tests. Prompt 3 is the one live moment of the talk — we run it on stage and watch Claude onboard a new archive provider end-to-end via the `/add-mock-provider` skill.

### Prompt 1 — content validation in the Noark A mock *(pre-merged)*

> Add always-on content validation to the Noark A mock: when an ENTRY_ADDED request body contains "error" (case-insensitive), return 400 with `{"error": "Content validation failed: entry contains forbidden text"}`. GROUP_CLOSED must not be affected. The NoarkAPayload currently doesn't include eventType — add it via TransformService so the mock can distinguish event types. Run `/testing-guide` and add tests at all levels. If anything is not clear, ask before starting.

### Prompt 2 — exponential backoff in Edge *(pre-merged)*

> Replace Edge's fixed 3-second retry with exponential backoff: 3s, 8s, 15s between attempts 1→2, 2→3, 3→4. Each `ArchiveGroup` needs to track when its next retry is due so the scheduler skips groups whose backoff hasn't elapsed. To test this with real timing, extend the mock's `POST /api/test/setup` with a `failResponses: number[]` field that returns those status codes for the next N requests, then falls back to 200. Add a FullPipelineIT scenario with `failResponses: [500, 500]` asserting the group reaches ARCHIVED only after ~11 seconds. Run `/testing-guide` and add tests at all levels. If anything is not clear, ask before starting.

### Prompt 3 — onboarding a new archive provider (Noark C) *(live on stage)*

> `/add-mock-provider use specs/noark-c.yaml`

## Hot-reload (development)

- **Angular:** `ng serve` watches files automatically
- **Spring Boot:** DevTools is included in all services. In IntelliJ, enable **Build > Build Project Automatically** and **Advanced Settings > Allow auto-make to start even if developed application is currently running** for automatic restarts on save.
