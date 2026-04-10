---
name: test-coverage-parity
description: Ensures Java FullPipelineIT and Playwright E2E tests maintain equivalent coverage when adding new functionality across microservices and frontend. Use this when implementing new features, error handling, or integration behavior that spans multiple services.
---

# Test Coverage Parity: Java FullPipelineIT + Playwright E2E

When adding new functionality to this application, you MUST add test coverage in BOTH test suites. They serve different purposes but must cover the same behavioral scenarios.

## The two test suites

### 1. Java FullPipelineIT (`edge/src/test/java/no/kantega/edge/FullPipelineIT.java`)

- Full-stack integration test using TestContainers (MongoDB, MySQL, RabbitMQ)
- Starts ALL microservices in-process: log-manager, edge, adapter-a, adapter-b, external-apis-mock
- Drives the pipeline through REST API calls (same endpoints the frontend uses)
- Asserts on Edge MongoDB state (status, retryCount, errors, entries) and mock request history
- The retry scheduler is DISABLED (`retry.scheduler.enabled=false`) — retries are triggered manually via `POST /api/retry`
- Tests run in ~30 seconds, no browser needed

### 2. Playwright E2E (`integration-tests/tests/`)

- Browser-based tests against the live running stack (`./start-all.sh`)
- Drives the pipeline through the Angular UI (clicking buttons, filling forms)
- Asserts on both UI state (data-testid selectors) and API state (direct HTTP requests)
- The retry scheduler IS ENABLED (runs every 3s) — tests must account for automatic retries
- All tests import from `tests/base-test.ts` which resets app state via the UI reset button before each test

## Current scenario coverage

| # | Scenario | Java test method | Playwright test file(s) |
|---|----------|-----------------|------------------------|
| 1 | Happy path: create, add entries, close -> ARCHIVED | `happyPath_createGroupAddEntriesClose_allArchived` | `happy-path/full-archive-flow.spec.ts` |
| 2 | Single adapter failure -> PENDING | `singleAdapterFailure_noarkAReturns500_groupPending` | `error-handling/single-adapter-failure-pending.spec.ts` |
| 3 | All adapters fail -> retries exhausted -> FAILED | `allAdaptersFail_retriesExhausted_groupFailed` | `retry/all-adapters-fail-retries-exhausted.spec.ts` |
| 4 | Retry succeeds after failure | `retrySucceeds_afterFailureMockReset_groupArchived` | `retry/both-fail-then-recover.spec.ts` |
| 5 | Multiple groups independence | `multipleGroups_failureInOneDoesNotAffectOther` | `error-handling/multiple-groups-independence.spec.ts` |
| 6 | Adapter event routing (A on every event, B only GROUP_CLOSED) | `adapterEventRouting_noarkAGetsEntryAdded_noarkBOnlyGroupClosed` | `adapter-behavior/adapter-event-routing.spec.ts` |
| 7 | 400 error code | `errorCode400_noarkAReturnsBadRequest_groupPending` | `error-handling/noarka-400-failure.spec.ts` |
| 8 | 503 error code | `errorCode503_noarkAReturnsServiceUnavailable_groupPending` | `error-handling/noarka-503-failure.spec.ts` |
| 9 | Noark-B-only failure | `noarkBOnlyFailure_noarkBReturns500_groupPending` | `error-handling/noarkb-500-failure.spec.ts` |
| 10 | Response delay behavior | `responseDelay_noarkASlowResponse_eventuallyArchived` | `mock-panel/noarka-delay-config.spec.ts` |

## How to add a new scenario

### Step 1: Add the Java test

Add a new `@Test` method to `FullPipelineIT.java`:

```java
@Test
@Order(11) // increment from last order number
void yourScenario_descriptiveName() throws Exception {
    // Configure mock if needed
    configureMock("noarka", 500, "{\"error\": \"down\"}");
    // or with delay:
    configureMockWithDelay("noarka", 200, 3000);

    // Drive the pipeline
    long groupId = createGroup("Your Group");
    addEntry(groupId, "Content");
    closeGroup(groupId);

    // Assert on Edge state (use Awaitility for async)
    await().atMost(TIMEOUT).pollInterval(POLL_INTERVAL).untilAsserted(() -> {
        JsonNode edgeGroup = getEdgeGroup(groupId);
        assertThat(edgeGroup.get("status").asText()).isEqualTo("ARCHIVED");
    });

    // Assert on mock history
    JsonNode history = getMockHistory();
    long count = countHistoryByEndpoint(history, "noarka");
    assertThat(count).isEqualTo(expectedCount);
}
```

Available helpers: `createGroup(name)`, `addEntry(groupId, content)`, `closeGroup(groupId)`, `getEdgeGroup(groupId)`, `getMockHistory()`, `countHistoryByEndpoint(history, endpoint)`, `configureMock(endpoint, statusCode, body)`, `configureMockWithDelay(endpoint, statusCode, delayMs)`, `triggerEdgeRetry()`.

**Remember:** The retry scheduler is disabled in this test. Use `triggerEdgeRetry()` to manually step through retries.

### Step 2: Add the Playwright test

Create a new spec file in the appropriate `integration-tests/tests/` subdirectory:

```typescript
import { test, expect, MOCK_URL, EDGE_URL } from '../base-test';

test.describe('Your Feature — Scenario Name', () => {
  test('description of what is tested', async ({ page, request }) => {
    // Configure mock if needed (AFTER base-test reset)
    await request.post(`${MOCK_URL}/api/test/setup`, {
      data: { endpoint: 'noarka', statusCode: 500 },
    });

    // Drive the pipeline through the UI
    await page.getByTestId('group-name-input').fill('Group Name');
    await page.getByTestId('create-group-button').click();
    // ... add entries, close group, etc.

    // Assert on UI state
    await expect(async () => {
      const edgeCards = page.locator('button[data-testid^="edge-group-"]');
      // ... find and check status
    }).toPass({ timeout: 30000 });

    // Assert on API state (optional, for deeper verification)
    const response = await request.get(`${EDGE_URL}/api/groups`);
    const groups = await response.json();
    // ... verify fields
  });
});
```

Key points:
- Import `test` and `expect` from `../base-test` (NOT from `@playwright/test`)
- The base fixture already navigates to the app, clicks RESET, and waits for empty lists
- Import constants you need: `MOCK_URL`, `EDGE_URL`, `LOG_MANAGER_URL`, `BASE_URL`
- The retry scheduler IS running — don't assert exact intermediate retryCount values, assert on final state or use `>=`
- Use `request` fixture for direct API calls to mock/edge services
- Use `page` for UI interactions and assertions

### Step 3: Verify both pass

```bash
# Java test
cd edge && mvn test -Dtest=FullPipelineIT

# Playwright tests
cd integration-tests && npx playwright test tests/your-new-test.spec.ts --trace on
```

## Key differences to account for

| Aspect | Java FullPipelineIT | Playwright E2E |
|--------|-------------------|----------------|
| Retry scheduler | DISABLED — manual `triggerEdgeRetry()` | ENABLED — runs every 3s automatically |
| State reset | `@BeforeEach` clears via API (mock reset, delete groups) | `base-test.ts` clicks UI reset button, waits for empty lists |
| Assertions | Exact values (retryCount=1, status=PENDING) | Relaxed where needed (retryCount>=1, status in [PENDING,FAILED]) |
| Mock config | `configureMock()` helper method | `request.post(MOCK_URL + '/api/test/setup', ...)` |
| Pipeline driver | REST API calls to log-manager | UI clicks via data-testid selectors |

## Mock API reference

- `POST /api/test/setup` — `{ endpoint: "noarka"|"noarkb", statusCode: number, body?: string, delayMs?: number }`
- `POST /api/test/reset` — reset to 200 OK defaults
- `GET /api/test/history` — array of `{ endpoint, method, path, timestamp, ... }`
- `GET /api/test/config` — current mock configuration

## Edge API reference

- `GET /api/groups` — all archive groups
- `GET /api/groups/{groupId}` — single group with status, retryCount, errors, entries, archiveEvents
- `POST /api/retry` — trigger retry for all PENDING groups
- `POST /api/groups/{groupId}/retry` — retry specific group
- `DELETE /api/groups` — delete all groups
