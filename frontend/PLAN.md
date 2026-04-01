# Frontend + Log Manager Backend — Implementation Plan

Implement PRD sections 6.1 (Frontend) and 6.2 (Log Manager Backend) together. You will be working in TWO projects:

- **Frontend:** `./` (this folder) — Angular app
- **Log Manager backend:** `../log-manager/` — Spring Boot 3 service, already scaffolded

## Layout

Split screen layout:

```
┌──────────────────────────┬──────────────────────────┐
│                          │                          │
│      LOG MANAGER         │    SYSTEM OVERVIEW       │
│                          │                          │
│  - Create new group      │  ┌─ RabbitMQ ──────────┐ │
│  - List all groups       │  │ messages in queue    │ │
│  - Add entry to group    │  └─────────────────────┘ │
│  - Close group           │  ┌─ MongoDB (Edge) ────┐ │
│  - View entries          │  │ archive states       │ │
│                          │  └─────────────────────┘ │
│                          │  ┌─ External APIs Mock ─┐ │
│                          │  │ received requests    │ │
│                          │  └─────────────────────┘ │
└──────────────────────────┴──────────────────────────┘
```

**Left side — Log Manager (PRD F1.a through F1.e):**
- Create a new log group (name required)
- List all groups (open and archived)
- Click a group to view its entries
- Add a log entry (string) to an open group
- Close a group (becomes read-only, marked archived)
- Show archiving status per group (pending, archived, failed)

**Right side — System Overview (PRD F1.f):**
Three read-only panels that poll their respective APIs:
- **RabbitMQ panel** — messages currently in the queue (poll RabbitMQ management API at localhost:15672, user: myuser, pass: secret)
- **MongoDB panel** — Edge's archive state per group (poll Edge status API at localhost:8081)
- **External APIs Mock panel** — request history from the mock (poll localhost:8084/api/test/history)

## Log Manager Backend — what to implement in `../log-manager/`

The Spring Boot project is already scaffolded with the right dependencies (Web, JPA, MySQL, RabbitMQ, Actuator, Lombok). The main class is `no.kantega.logmanager.LogManagerApplication`. Config is in `application.properties` (MySQL and RabbitMQ already configured).

### REST API

```
POST   /api/groups              — create a new group { name: string }
GET    /api/groups              — list all groups
GET    /api/groups/{id}         — get a single group with its entries
POST   /api/groups/{id}/entries — add entry { content: string }
PUT    /api/groups/{id}/close   — close/archive a group
```

### MySQL entities

- **LogGroup** — id (Long, auto), name (String), status (enum: OPEN, ARCHIVED), createdAt, updatedAt
- **LogEntry** — id (Long, auto), logGroup (ManyToOne), content (String), createdAt

### RabbitMQ publishing

Every mutation must publish a message to RabbitMQ:
- Group created → publish `{ event: "GROUP_CREATED", groupId, name, timestamp }`
- Entry added → publish `{ event: "ENTRY_ADDED", groupId, entryId, content, timestamp }`
- Group closed → publish `{ event: "GROUP_CLOSED", groupId, timestamp }`

Use a single exchange/queue (e.g. exchange: `log-events`, routing key: `log.events`).

### Backend tests

Write unit tests for the service layer and controller layer using Spring Boot Test + MockMvc. Mock the RabbitMQ template in tests.

## Tech details

- Frontend Angular project: already initialized in this folder
- Log Manager backend: `../log-manager/`, Spring Boot 3.5.x, Java 17, package `no.kantega.logmanager`
- Edge status API at localhost:8081 — not yet implemented, frontend should handle gracefully
- External APIs Mock at localhost:8084 — not yet implemented, frontend should handle gracefully
- RabbitMQ Management API at localhost:15672 — available via docker-compose

## Guidelines

- Frontend: no unit tests needed (Playwright tests will be added later)
- Frontend: handle unavailable backends gracefully — show "service unavailable" per panel, don't crash
- Frontend: clean and simple UI, functional and readable, this is for a technical presentation
- Backend: write unit tests for service and controller layers
- Both projects must work together end-to-end when started
