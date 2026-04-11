---
name: testing-guide
description: Guide for adding tests at all three levels (unit, Java integration, Playwright E2E) when implementing new functionality across microservices and frontend. Use this when adding features, error handling, or integration behavior.
---

# Test Coverage: Unit Tests + Java FullPipelineIT + Playwright E2E

When adding new functionality to this application, you MUST add test coverage at **three levels**. Each level catches different classes of bugs.

## The three test levels

### 1. Unit tests (per-service, fast, isolated)

- Each service has its own unit tests under `<service>/src/test/java/`
- Test individual classes in isolation using Mockito mocks for dependencies
- Run in milliseconds — the first line of defense for logic bugs
- Two patterns are used:
  - **Service tests** (`@ExtendWith(MockitoExtension.class)`) — test business logic with mocked repositories and clients
  - **Controller tests** (`@WebMvcTest`) — test HTTP layer with MockMvc and mocked service beans

#### Existing unit tests by service

| Service | Test files | What they cover |
|---------|-----------|----------------|
| `log-manager/` | `LogManagerServiceTest`, `RabbitMQPublisherTest`, `LogGroupControllerTest` | CRUD logic, event publishing, HTTP endpoints |
| `edge/` | `ArchiveServiceTest`, `LogEventListenerTest`, `StatusControllerTest` | Adapter routing, retry/status transitions, event handling |
| `adapter-noark-a/` | `ArchiveControllerTest`, `ArchiveServiceTest`, `TransformServiceTest` | Archive endpoint, Noark A client, JSON transform |
| `adapter-noark-b/` | `ArchiveControllerTest`, `ArchiveServiceTest`, `ZipServiceTest` | Archive endpoint, Noark B client, ZIP packaging |
| `external-apis-mock/` | `NoarkAControllerTest`, `TestControllerTest`, `MockServiceTest` | Mock endpoints, setup/reset/history API |

All services also have `OpenApiBackwardsCompatibilityTest` to guard against accidental API contract changes.

### 2. Java FullPipelineIT (`edge/src/test/java/no/kantega/edge/FullPipelineIT.java`)

- Full-stack integration test using TestContainers (MongoDB, MySQL, RabbitMQ)
- Starts ALL microservices in-process: log-manager, edge, adapter-a, adapter-b, external-apis-mock
- Drives the pipeline through REST API calls (same endpoints the frontend uses)
- Asserts on Edge MongoDB state (status, retryCount, errors, entries) and mock request history
- The retry scheduler is DISABLED (`retry.scheduler.enabled=false`) — retries are triggered manually via `POST /api/retry`
- Tests run in ~30 seconds, no browser needed

### 3. Playwright E2E (`integration-tests/tests/`)

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

### Step 1: Add unit tests for every service you changed

For each service where you added or modified logic, add unit tests in the corresponding test directory. Follow the existing patterns:

**Service logic** — use `@ExtendWith(MockitoExtension.class)` with `@Mock` dependencies:

```java
@ExtendWith(MockitoExtension.class)
class YourServiceTest {

    @Mock
    private SomeDependency dependency;

    // Create the service under test manually (inject mocks via constructor)
    private YourService createService() {
        return new YourService(dependency);
    }

    @Test
    void yourMethod_givenCondition_expectedBehavior() {
        YourService service = createService();
        when(dependency.doSomething(any())).thenReturn(expectedValue);

        var result = service.yourMethod(input);

        assertThat(result).isEqualTo(expected);
        verify(dependency).doSomething(any());
    }
}
```

**Controller/HTTP layer** — use `@WebMvcTest` with `@MockitoBean` services:

```java
@WebMvcTest(YourController.class)
class YourControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private YourService yourService;

    @Test
    void endpoint_returnsExpectedResponse() throws Exception {
        when(yourService.doSomething(any())).thenReturn(result);

        mockMvc.perform(post("/your/endpoint")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"field\":\"value\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.field").value("expected"));
    }
}
```

**What to unit test:**
- New business logic (validation, transformation, state transitions)
- New or changed controller endpoints (status codes, response shapes, error cases)
- Edge cases and error paths that are hard to trigger in integration tests
- Any conditional behavior (if/else branches, switch cases)

**Run unit tests:**

```bash
cd <service> && mvn test                          # all tests in a service
cd <service> && mvn test -Dtest=YourServiceTest   # specific test class
```

### Step 2: Add the Java integration test

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

### Step 3: Add the Playwright test

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

### Step 4: Verify all three levels pass

```bash
# Unit tests — run for each service you changed
cd log-manager && mvn test
cd edge && mvn test -Dtest='!FullPipelineIT'   # unit tests only (exclude IT)
cd adapter-noark-a && mvn test
cd adapter-noark-b && mvn test
cd external-apis-mock && mvn test

# Java integration test
cd edge && mvn test -Dtest=FullPipelineIT

# Playwright E2E tests
cd integration-tests && npx playwright test tests/your-new-test.spec.ts --trace on
```

## Key differences to account for

| Aspect | Unit tests | Java FullPipelineIT | Playwright E2E |
|--------|-----------|-------------------|----------------|
| Scope | Single class, mocked dependencies | All services + real infra (TestContainers) | Full stack + browser |
| Speed | Milliseconds | ~30 seconds | Minutes |
| Retry scheduler | N/A (mocked) | DISABLED — manual `triggerEdgeRetry()` | ENABLED — runs every 3s automatically |
| State reset | Each test creates its own mocks | `@BeforeEach` clears via API | `base-test.ts` clicks UI reset button |
| Assertions | Exact, on return values and mock interactions | Exact (retryCount=1, status=PENDING) | Relaxed where needed (retryCount>=1) |
| What they catch | Logic bugs, edge cases, regressions | Integration bugs, wiring, data flow | UI bugs, user-facing behavior |

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
