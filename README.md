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
├── docker-compose.yml        # MySQL, MongoDB, RabbitMQ
└── start-all.sh              # Start/stop everything
```

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

## Hot-reload (development)

- **Angular:** `ng serve` watches files automatically
- **Spring Boot:** DevTools is included in all services. In IntelliJ, enable **Build > Build Project Automatically** and **Advanced Settings > Allow auto-make to start even if developed application is currently running** for automatic restarts on save.
