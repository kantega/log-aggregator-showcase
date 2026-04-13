# Presentation: Claude & The Mystery of Integrations

Slidev presentation about how Claude Code skills help build and test integrations with external systems you can't control.

## Running

```bash
cd presentation && npm run dev
```

## Narrative arc — 6 acts

1. **The Application** — Simple app: Frontend + Log Manager. Just a log group CRUD system, no archiving yet.
2. **The New Requirement** — Customers need archiving; each uses their own provider (Noark A, Noark B). Introduces Edge service, RabbitMQ, adapter pattern. Shows the complexity and the problem with real providers (no test env, or one that only returns success).
3. **The Mock Server** — A fake version of external systems we control.
4. **This Is a Lot of Work** — Building/maintaining mocks is repetitive. Introduces Claude Code skills. **Skill 1: /add-mock-provider** — given an OpenAPI YAML, generates full mock for a new provider. **Skill 2: /testing-guide** — writes tests at three levels. Culminates in "skills working together" slide.
5. **Demo** — Live: give Claude an OpenAPI spec, skills generate mocks and tests.
6. **Why This Matters** — Skills as codified team knowledge. Benefits: consistency, speed, knowledge sharing, onboarding.

## What's done

- All 6 microservices built and working end-to-end
- Both skills exist and work: `/add-mock-provider` and `/testing-guide`
- Java FullPipelineIT — 10 scenarios, all passing (Testcontainers with MongoDB, MySQL, RabbitMQ)
- Playwright E2E — 42 test files across 11 functional areas
- Unit tests — complete across all services
- Slidev deck — all 6 acts written, uses `<v-clicks>` for progressive reveal
- API docs — SpringDoc OpenAPI/Swagger UI on all services

## What's still open

1. **Two Playwright tests** not yet written: error handling E2E and exponential backoff E2E
2. **Skills for the live demo** — PLAN.md mentions "add retry logic, create integration test, setup mock behavior" as incomplete
3. **Demo rehearsal** — Act 5 (live demo) hasn't been fully rehearsed with the actual skills
4. The exponential backoff test (planned "live coding on stage" moment) doesn't exist yet as a Playwright test

## Key context

- The demo (Act 5) is the centerpiece of the talk
- The app is a SaaS platform where different customers use different archive providers (each provider is a SaaS solution on its own infrastructure)
- Two skills are featured: `/add-mock-provider` (generates mock endpoints from OpenAPI spec) and `/testing-guide` (writes tests at unit, Java integration, Playwright E2E levels)
- Slides use `<v-clicks>` for progressive reveal, Slidev default theme
