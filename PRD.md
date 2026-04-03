# PRD: Log Manager

## 1. Problem / Opportunity

Organizations need to archive log data to external NOARK-compliant archiving systems. These external systems are on the customer side — we only have their API contracts (Swagger specs) and no control over their reliability or behavior. We need a system that reliably creates, manages, and archives logs while handling failures from these external dependencies gracefully.

---

## 2. Target Users & Use Cases

| User | Use Case |
|------|----------|
| **Log Manager end-user** | Creates log groups, adds log entries (strings), closes groups, and views archiving status |
| **Operations / Support** | Monitors archiving state, identifies failed archives, investigates errors |

---

## 3. Current Landscape

- Two official NOARK standard implementations exist (Noark A, Noark B), hosted on the customer side
- Noark A accepts JSON payloads via POST
- Noark B accepts ZIP files via POST
- We only have Swagger specs — no control over uptime, error behavior, or internals

---

## 4. Proposed Solution / Elevator Pitch

A microservice-based log management system where users create and manage log groups through a web UI. Every event (group creation, new log entry, group close) is forwarded through a message queue to an archiving buffer service ("Edge"), which routes data to adapter services based on configurable trigger types — some adapters receive individual entries immediately, others receive the full group on close. Each adapter transforms the data to match its target NOARK system's API contract, keeping Edge decoupled from external implementation details.

---

## 5. Architecture Overview

```
┌─────────────────────┐
│   Angular Frontend   │
└─────────┬───────────┘
          │
┌─────────▼───────────┐
│  Log Manager (API)   │  Spring Boot 3 + MySQL
└─────────┬───────────┘
          │ RabbitMQ
┌─────────▼───────────┐
│        Edge          │  Spring Boot 3 + MongoDB
│  (Buffer / Router)   │
└────┬───────────┬────┘
     │           │
┌────▼────┐ ┌───▼─────┐
│Adapter A│ │Adapter B│  Spring Boot 3 each
│(per-entry)│(per-group)│
└────┬────┘ └───┬─────┘
     │          │
  Noark A    Noark B     (external, customer-hosted)
```

---

## 6. Functional Requirements

### 6.1 Log Manager — Frontend (Angular)

| ID | Priority | Requirement |
|----|----------|-------------|
| F1.a | P0 | User can create a new log group (name required) |
| F1.b | P0 | User can add log entries (string only) to an open group |
| F1.c | P0 | User can close a log group (marks as archived, becomes read-only) |
| F1.d | P1 | User can browse all groups (open and archived) and view their entries |
| F1.e | P1 | UI shows archiving status per group (pending, archived, failed) |
| F1.f | P0 | Layout: left side — Log Manager (groups, entries, actions); right side — live overview panels showing current state of RabbitMQ, MongoDB (Edge), and external-apis-mock |

### 6.2 Log Manager — Backend (Spring Boot 3 + MySQL)

| ID | Priority | Requirement |
|----|----------|-------------|
| F2.a | P0 | REST API: CRUD for log groups (create, get, list, close) |
| F2.b | P0 | REST API: Add log entry to a group |
| F2.c | P0 | On every event (group created, log entry added, group closed), publish message to RabbitMQ |
| F2.d | P0 | MySQL schema: groups table (id, name, status, timestamps), entries table (id, group_id, content, timestamp) |
| F2.e | P1 | Endpoint to query archiving status (relayed from Edge) |

### 6.3 Edge — Buffer / Router (Spring Boot 3 + MongoDB)

| ID | Priority | Requirement |
|----|----------|-------------|
| F3.a | P0 | Consume messages from RabbitMQ (group events, log entry events) |
| F3.b | P0 | Maintain archive state in MongoDB (per group: pending, in-progress, archived, failed) |
| F3.c | P0 | Route data to adapters based on configurable trigger type: ON_ENTRY adapters receive each entry immediately when added; ON_GROUP_CLOSE adapters receive the full group when closed. Edge is adapter-agnostic — it has no knowledge of Noark A/B implementation details |
| F3.c2 | P0 | Track per-entry, per-adapter archive status (pending, in-progress, archived, failed) using a generic adapter status map |
| F3.d | P0 | Record errors from adapters in MongoDB with full context |
| F3.e | P1 | Expose status API for Log Manager to query archiving progress |
| F3.f | P1 | Retry failed archiving attempts (basic retry) |

### 6.4 Adapter Noark A (Spring Boot 3)

| ID | Priority | Requirement |
|----|----------|-------------|
| F4.a | P0 | Receive per-entry archive requests from Edge (triggered on each entry add) |
| F4.b | P0 | Transform data into JSON payload per Noark A Swagger spec |
| F4.c | P0 | POST JSON to Noark A endpoint (configurable target URL) |
| F4.d | P0 | Return success/failure result to Edge |

### 6.5 Adapter Noark B (Spring Boot 3)

| ID | Priority | Requirement |
|----|----------|-------------|
| F5.a | P0 | Receive per-group archive requests from Edge (triggered on group close, contains all entries) |
| F5.b | P0 | Transform data into ZIP file per Noark B Swagger spec |
| F5.c | P0 | POST ZIP to Noark B endpoint (configurable target URL) |
| F5.d | P0 | Return success/failure result to Edge |

### 6.6 external-apis-mock (Spring Boot 3)

| ID | Priority | Requirement |
|----|----------|-------------|
| F6.a | P0 | `/api/noarka/*` — Accept and respond to all Noark A API calls (default: 200 OK) |
| F6.b | P0 | `/api/noarkb/*` — Accept and respond to all Noark B API calls (default: 200 OK) |
| F6.c | P0 | `/api/test/setup` — Configure mock behavior at runtime: response status codes, delays, error payloads per endpoint |
| F6.d | P0 | `/api/test/reset` — Reset mock to default happy-path behavior |
| F6.e | P1 | `/api/test/history` — Return log of all received requests (for test assertions) |
| F6.f | P1 | Support configurable response delays (for timeout testing) |

---

## 7. Non-Functional Requirements

| ID | Priority | Requirement |
|----|----------|-------------|
| NF1 | P0 | All services runnable locally via Docker Compose |
| NF2 | P0 | Each service has unit tests |
| NF3 | P1 | Integration tests runnable with a single command |

---

## Appendix

- **Noark A Swagger:** TBD
- **Noark B Swagger:** TBD
