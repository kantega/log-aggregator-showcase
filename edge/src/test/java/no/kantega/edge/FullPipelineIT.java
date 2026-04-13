package no.kantega.edge;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.kantega.adapternoraka.AdapterNoarkAApplication;
import no.kantega.adapternorakb.AdapterNoarkBApplication;
import no.kantega.edge.service.ArchiveService;
import no.kantega.externalapismock.ExternalApisMockApplication;
import no.kantega.logmanager.LogManagerApplication;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.web.client.RestClient;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.lifecycle.Startables;

import java.time.Duration;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Full-stack integration test that starts ALL microservices and infrastructure via TestContainers.
 * <p>
 * Uses {@code @SpringBootTest} for the Edge service (Spring manages and caches the context),
 * while auxiliary services (log-manager, adapter-a, adapter-b, external-apis-mock) are started
 * once in a static initializer and shared across all test classes that use the same configuration.
 * <p>
 * The test drives the pipeline through log-manager's REST API (same as the Angular frontend)
 * and verifies state in Edge's MongoDB and the external-apis-mock request history.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EnableAutoConfiguration(exclude = {
        DataSourceAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class
})
@ContextConfiguration(initializers = FullPipelineIT.InfrastructureInitializer.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FullPipelineIT {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final Duration TIMEOUT = Duration.ofSeconds(30);
    private static final Duration POLL_INTERVAL = Duration.ofMillis(500);

    // ==================== STATIC INFRASTRUCTURE ====================
    // Started once, shared across all test classes that use InfrastructureInitializer.

    static MongoDBContainer mongodb = new MongoDBContainer("mongo:7");
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8")
            .withDatabaseName("logmanager")
            .withUsername("myuser")
            .withPassword("secret");
    static RabbitMQContainer rabbitmq = new RabbitMQContainer("rabbitmq:3-management")
            .withUser("myuser", "secret")
            .withPermission("/", "myuser", ".*", ".*", ".*");

    static ConfigurableApplicationContext mockCtx;
    static ConfigurableApplicationContext adapterACtx;
    static ConfigurableApplicationContext adapterBCtx;
    static ConfigurableApplicationContext logManagerCtx;

    static int mockPort;
    static int adapterAPort;
    static int adapterBPort;
    static int logManagerPort;

    // Auto-configuration exclusions for auxiliary services
    private static final String EXCLUDE_JPA_AND_MONGO = "--spring.autoconfigure.exclude=" +
            "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration," +
            "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration," +
            "org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration," +
            "org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration," +
            "org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration";

    private static final String EXCLUDE_MONGO = "--spring.autoconfigure.exclude=" +
            "org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration," +
            "org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration";

    /**
     * Initializer that starts TestContainers + auxiliary services ONCE, then injects
     * the dynamic properties into Edge's Spring context.
     */
    static class InfrastructureInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(ConfigurableApplicationContext ctx) {
            // Start all containers in parallel
            Startables.deepStart(mongodb, mysql, rabbitmq).join();

            // Start auxiliary services in dependency order (only once)
            if (mockCtx == null) {
                mockCtx = startAuxService(ExternalApisMockApplication.class,
                        "--server.port=0",
                        "--spring.docker.compose.enabled=false",
                        EXCLUDE_JPA_AND_MONGO);
                mockPort = extractPort(mockCtx);

                adapterACtx = startAuxService(AdapterNoarkAApplication.class,
                        "--server.port=0",
                        "--spring.docker.compose.enabled=false",
                        "--noark-a.api.url=http://localhost:" + mockPort + "/api/noarka",
                        EXCLUDE_JPA_AND_MONGO);
                adapterAPort = extractPort(adapterACtx);

                adapterBCtx = startAuxService(AdapterNoarkBApplication.class,
                        "--server.port=0",
                        "--spring.docker.compose.enabled=false",
                        "--noark-b.api.url=http://localhost:" + mockPort + "/api/noarkb",
                        EXCLUDE_JPA_AND_MONGO);
                adapterBPort = extractPort(adapterBCtx);

                logManagerCtx = startAuxService(LogManagerApplication.class,
                        "--server.port=0",
                        "--spring.docker.compose.enabled=false",
                        EXCLUDE_MONGO,
                        "--spring.datasource.url=" + mysql.getJdbcUrl(),
                        "--spring.datasource.username=" + mysql.getUsername(),
                        "--spring.datasource.password=" + mysql.getPassword(),
                        "--spring.jpa.hibernate.ddl-auto=update",
                        "--spring.rabbitmq.host=" + rabbitmq.getHost(),
                        "--spring.rabbitmq.port=" + rabbitmq.getAmqpPort(),
                        "--spring.rabbitmq.username=myuser",
                        "--spring.rabbitmq.password=secret");
                logManagerPort = extractPort(logManagerCtx);
            }

            // Inject dynamic properties into Edge's Spring context
            TestPropertyValues.of(
                    "spring.docker.compose.enabled=false",
                    "spring.data.mongodb.uri=" + mongodb.getReplicaSetUrl("edge"),
                    "spring.rabbitmq.host=" + rabbitmq.getHost(),
                    "spring.rabbitmq.port=" + rabbitmq.getAmqpPort(),
                    "spring.rabbitmq.username=myuser",
                    "spring.rabbitmq.password=secret",
                    "adapters[0].name=adapter-a",
                    "adapters[0].url=http://localhost:" + adapterAPort,
                    "adapters[1].name=adapter-b",
                    "adapters[1].url=http://localhost:" + adapterBPort,
                    "retry.scheduler.enabled=false"
            ).applyTo(ctx.getEnvironment());
        }

        private static ConfigurableApplicationContext startAuxService(Class<?> mainClass, String... args) {
            SpringApplication app = new SpringApplication(mainClass);
            app.setAdditionalProfiles("integration-test");
            return app.run(args);
        }

        private static int extractPort(ConfigurableApplicationContext ctx) {
            return ctx.getBean(Environment.class)
                    .getProperty("local.server.port", Integer.class, 0);
        }
    }

    // ==================== INSTANCE FIELDS (injected by Spring) ====================

    @Value("${local.server.port}")
    private int edgePort;

    @Autowired
    private ArchiveService archiveService;

    private final RestClient restClient = RestClient.create();

    // ==================== LIFECYCLE ====================

    @BeforeEach
    void resetState() {
        // Reset mock configuration and history
        restClient.post()
                .uri("http://localhost:" + mockPort + "/api/test/reset")
                .retrieve().toBodilessEntity();

        // Clear edge MongoDB
        restClient.delete()
                .uri("http://localhost:" + edgePort + "/api/groups")
                .retrieve().toBodilessEntity();

        // Clear log-manager MySQL
        restClient.delete()
                .uri("http://localhost:" + logManagerPort + "/api/groups")
                .retrieve().toBodilessEntity();
    }

    // ==================== SCENARIO 1: HAPPY PATH ====================

    @Test
    @Order(1)
    void happyPath_createGroupAddEntriesClose_allArchived() throws Exception {
        // Create group
        long groupId = createGroup("Happy Group");

        // Add 2 entries
        addEntry(groupId, "First log entry");
        addEntry(groupId, "Second log entry");

        // Close group
        closeGroup(groupId);

        // Wait for edge to process and archive
        await().atMost(TIMEOUT).pollInterval(POLL_INTERVAL).untilAsserted(() -> {
            JsonNode edgeGroup = getEdgeGroup(groupId);
            assertThat(edgeGroup.get("status").asText()).isEqualTo("ARCHIVED");
        });

        // Verify edge MongoDB state
        JsonNode edgeGroup = getEdgeGroup(groupId);
        assertThat(edgeGroup.get("entries").size()).isEqualTo(2);
        assertThat(edgeGroup.get("errors").size()).isEqualTo(0);
        assertThat(edgeGroup.get("name").asText()).isEqualTo("Happy Group");

        // Verify mock history
        JsonNode history = getMockHistory();
        // adapter-a sends on ENTRY_ADDED (x2) + GROUP_CLOSED (x1) = 3 noarka requests
        long noarkaCount = countHistoryByEndpoint(history, "noarka");
        assertThat(noarkaCount).isEqualTo(3);

        // adapter-b only sends on GROUP_CLOSED = 1 noarkb request
        long noarkbCount = countHistoryByEndpoint(history, "noarkb");
        assertThat(noarkbCount).isEqualTo(1);
    }

    // ==================== SCENARIO 2: SINGLE ADAPTER FAILURE ====================

    @Test
    @Order(2)
    void singleAdapterFailure_noarkAReturns500_groupPending() throws Exception {
        // Configure noark-a to fail
        configureMock("noarka", 500, "{\"error\": \"internal error\"}");

        // Create group, add entry, close
        long groupId = createGroup("Partial Failure Group");
        addEntry(groupId, "Some content");
        closeGroup(groupId);

        // Wait for edge to process
        await().atMost(TIMEOUT).pollInterval(POLL_INTERVAL).untilAsserted(() -> {
            JsonNode edgeGroup = getEdgeGroup(groupId);
            assertThat(edgeGroup.get("status").asText()).isEqualTo("PENDING");
        });

        // Verify state
        JsonNode edgeGroup = getEdgeGroup(groupId);
        assertThat(edgeGroup.get("retryCount").asInt()).isEqualTo(1);
        assertThat(edgeGroup.get("errors").size()).isGreaterThan(0);

        // noark-b should still have received data
        JsonNode history = getMockHistory();
        long noarkbCount = countHistoryByEndpoint(history, "noarkb");
        assertThat(noarkbCount).isGreaterThanOrEqualTo(1);
    }

    // ==================== SCENARIO 3: ALL ADAPTERS FAIL → FAILED ====================

    @Test
    @Order(3)
    void allAdaptersFail_retriesExhausted_groupFailed() throws Exception {
        // Configure both to fail
        configureMock("noarka", 500, "{\"error\": \"down\"}");
        configureMock("noarkb", 500, "{\"error\": \"down\"}");

        // Create group, add entry, close
        long groupId = createGroup("Total Failure Group");
        addEntry(groupId, "Will fail");
        closeGroup(groupId);

        // Wait for PENDING status after first attempt (retryCount=1)
        await().atMost(TIMEOUT).pollInterval(POLL_INTERVAL).untilAsserted(() -> {
            JsonNode edgeGroup = getEdgeGroup(groupId);
            assertThat(edgeGroup.get("status").asText()).isEqualTo("PENDING");
            assertThat(edgeGroup.get("retryCount").asInt()).isEqualTo(1);
        });

        // Trigger retry #2 (retryCount goes to 2)
        triggerEdgeRetry();
        await().atMost(TIMEOUT).pollInterval(POLL_INTERVAL).untilAsserted(() -> {
            JsonNode edgeGroup = getEdgeGroup(groupId);
            assertThat(edgeGroup.get("retryCount").asInt()).isEqualTo(2);
        });

        // Trigger retry #3 (retryCount=3, still < MAX_RETRIES=4 → PENDING)
        triggerEdgeRetry();
        await().atMost(TIMEOUT).pollInterval(POLL_INTERVAL).untilAsserted(() -> {
            JsonNode edgeGroup = getEdgeGroup(groupId);
            assertThat(edgeGroup.get("status").asText()).isEqualTo("PENDING");
            assertThat(edgeGroup.get("retryCount").asInt()).isEqualTo(3);
        });

        // Trigger retry #4 (retryCount reaches MAX_RETRIES=4 → FAILED)
        triggerEdgeRetry();
        await().atMost(TIMEOUT).pollInterval(POLL_INTERVAL).untilAsserted(() -> {
            JsonNode edgeGroup = getEdgeGroup(groupId);
            assertThat(edgeGroup.get("status").asText()).isEqualTo("FAILED");
            assertThat(edgeGroup.get("retryCount").asInt()).isEqualTo(4);
        });
    }

    // ==================== SCENARIO 4: RETRY SUCCEEDS ====================

    @Test
    @Order(4)
    void retrySucceeds_afterFailureMockReset_groupArchived() throws Exception {
        // Configure both to fail
        configureMock("noarka", 500, "{\"error\": \"down\"}");
        configureMock("noarkb", 500, "{\"error\": \"down\"}");

        // Create group, add entry, close
        long groupId = createGroup("Retry Success Group");
        addEntry(groupId, "Will eventually succeed");
        closeGroup(groupId);

        // Wait for PENDING
        await().atMost(TIMEOUT).pollInterval(POLL_INTERVAL).untilAsserted(() -> {
            JsonNode edgeGroup = getEdgeGroup(groupId);
            assertThat(edgeGroup.get("status").asText()).isEqualTo("PENDING");
        });

        // Reset mock to default (200 OK)
        restClient.post()
                .uri("http://localhost:" + mockPort + "/api/test/reset")
                .retrieve().toBodilessEntity();

        // Trigger retry — should now succeed
        triggerEdgeRetry();

        await().atMost(TIMEOUT).pollInterval(POLL_INTERVAL).untilAsserted(() -> {
            JsonNode edgeGroup = getEdgeGroup(groupId);
            assertThat(edgeGroup.get("status").asText()).isEqualTo("ARCHIVED");
        });
    }

    // ==================== SCENARIO 5: MULTIPLE GROUPS INDEPENDENCE ====================

    @Test
    @Order(5)
    void multipleGroups_failureInOneDoesNotAffectOther() throws Exception {
        // Configure noark-a to fail
        configureMock("noarka", 500, "{\"error\": \"down\"}");

        // Create group A — will fail on close
        long groupAId = createGroup("Group A - Will Fail");
        addEntry(groupAId, "Entry for A");
        closeGroup(groupAId);

        await().atMost(TIMEOUT).pollInterval(POLL_INTERVAL).untilAsserted(() -> {
            JsonNode edgeGroup = getEdgeGroup(groupAId);
            assertThat(edgeGroup.get("status").asText()).isEqualTo("PENDING");
        });

        // Reset mock — now everything works
        restClient.post()
                .uri("http://localhost:" + mockPort + "/api/test/reset")
                .retrieve().toBodilessEntity();

        // Create group B — should succeed
        long groupBId = createGroup("Group B - Will Succeed");
        addEntry(groupBId, "Entry for B");
        closeGroup(groupBId);

        await().atMost(TIMEOUT).pollInterval(POLL_INTERVAL).untilAsserted(() -> {
            JsonNode edgeGroup = getEdgeGroup(groupBId);
            assertThat(edgeGroup.get("status").asText()).isEqualTo("ARCHIVED");
        });

        // Confirm group A is still PENDING
        JsonNode groupA = getEdgeGroup(groupAId);
        assertThat(groupA.get("status").asText()).isEqualTo("PENDING");
    }

    // ==================== SCENARIO 6: ADAPTER EVENT ROUTING ====================

    @Test
    @Order(6)
    void adapterEventRouting_noarkAGetsEntryAdded_noarkBOnlyGroupClosed() throws Exception {
        // Create group and add 2 entries (do NOT close yet)
        long groupId = createGroup("Routing Group");
        addEntry(groupId, "First entry");
        addEntry(groupId, "Second entry");

        // Wait for adapter-a to process ENTRY_ADDED events
        await().atMost(TIMEOUT).pollInterval(POLL_INTERVAL).untilAsserted(() -> {
            JsonNode history = getMockHistory();
            long noarkaCount = countHistoryByEndpoint(history, "noarka");
            assertThat(noarkaCount).isEqualTo(2);
        });

        // Before close: noark-b should have received 0 requests
        JsonNode historyBeforeClose = getMockHistory();
        long noarkbBeforeClose = countHistoryByEndpoint(historyBeforeClose, "noarkb");
        assertThat(noarkbBeforeClose).isEqualTo(0);

        // Close the group
        closeGroup(groupId);

        // Wait for both adapters to process GROUP_CLOSED
        await().atMost(TIMEOUT).pollInterval(POLL_INTERVAL).untilAsserted(() -> {
            JsonNode history = getMockHistory();
            // noark-a: 2 ENTRY_ADDED + 1 GROUP_CLOSED = 3
            assertThat(countHistoryByEndpoint(history, "noarka")).isEqualTo(3);
            // noark-b: 1 GROUP_CLOSED only
            assertThat(countHistoryByEndpoint(history, "noarkb")).isEqualTo(1);
        });
    }

    // ==================== SCENARIO 7: 400 ERROR CODE ====================

    @Test
    @Order(7)
    void errorCode400_noarkAReturnsBadRequest_groupPending() throws Exception {
        configureMock("noarka", 400, "{\"error\": \"bad request\"}");

        long groupId = createGroup("Bad Request Group");
        addEntry(groupId, "400 content");
        closeGroup(groupId);

        await().atMost(TIMEOUT).pollInterval(POLL_INTERVAL).untilAsserted(() -> {
            JsonNode edgeGroup = getEdgeGroup(groupId);
            assertThat(edgeGroup.get("status").asText()).isEqualTo("PENDING");
        });

        JsonNode edgeGroup = getEdgeGroup(groupId);
        assertThat(edgeGroup.get("retryCount").asInt()).isEqualTo(1);
        assertThat(edgeGroup.get("errors").size()).isGreaterThan(0);
    }

    // ==================== SCENARIO 8: 503 ERROR CODE ====================

    @Test
    @Order(8)
    void errorCode503_noarkAReturnsServiceUnavailable_groupPending() throws Exception {
        configureMock("noarka", 503, "{\"error\": \"service unavailable\"}");

        long groupId = createGroup("Unavailable Group");
        addEntry(groupId, "503 content");
        closeGroup(groupId);

        await().atMost(TIMEOUT).pollInterval(POLL_INTERVAL).untilAsserted(() -> {
            JsonNode edgeGroup = getEdgeGroup(groupId);
            assertThat(edgeGroup.get("status").asText()).isEqualTo("PENDING");
        });

        JsonNode edgeGroup = getEdgeGroup(groupId);
        assertThat(edgeGroup.get("retryCount").asInt()).isEqualTo(1);
        assertThat(edgeGroup.get("errors").size()).isGreaterThan(0);
    }

    // ==================== SCENARIO 9: NOARK-B ONLY FAILURE ====================

    @Test
    @Order(9)
    void noarkBOnlyFailure_noarkBReturns500_groupPending() throws Exception {
        // Only noark-b fails — noark-a stays healthy
        configureMock("noarkb", 500, "{\"error\": \"internal error\"}");

        long groupId = createGroup("NoarkB Failure Group");
        addEntry(groupId, "B-fail content");
        closeGroup(groupId);

        await().atMost(TIMEOUT).pollInterval(POLL_INTERVAL).untilAsserted(() -> {
            JsonNode edgeGroup = getEdgeGroup(groupId);
            assertThat(edgeGroup.get("status").asText()).isEqualTo("PENDING");
        });

        JsonNode edgeGroup = getEdgeGroup(groupId);
        assertThat(edgeGroup.get("retryCount").asInt()).isEqualTo(1);
        assertThat(edgeGroup.get("errors").size()).isGreaterThan(0);

        // noark-a should still have received its requests
        JsonNode history = getMockHistory();
        long noarkaCount = countHistoryByEndpoint(history, "noarka");
        assertThat(noarkaCount).isGreaterThanOrEqualTo(1);
    }

    // ==================== SCENARIO 10: RESPONSE DELAY ====================

    @Test
    @Order(10)
    void responseDelay_noarkASlowResponse_eventuallyArchived() throws Exception {
        // Configure noark-a with a 3-second delay
        configureMockWithDelay("noarka", 200, 3000);

        long groupId = createGroup("Delay Group");
        addEntry(groupId, "Delayed content");
        closeGroup(groupId);

        // Immediately after close, the group should NOT be ARCHIVED yet
        // (the 3s delay on noark-a means archiving is still in progress)
        Thread.sleep(500);
        JsonNode earlyState = getEdgeGroup(groupId);
        assertThat(earlyState.get("status").asText()).isNotEqualTo("ARCHIVED");

        // Eventually it should reach ARCHIVED once the delay completes
        await().atMost(TIMEOUT).pollInterval(POLL_INTERVAL).untilAsserted(() -> {
            JsonNode edgeGroup = getEdgeGroup(groupId);
            assertThat(edgeGroup.get("status").asText()).isEqualTo("ARCHIVED");
        });
    }

    // ==================== SCENARIO 11: CONTENT VALIDATION ====================

    @Test
    @Order(11)
    void contentValidation_entryWithForbiddenText_returns400ButGroupClosedUnaffected() throws Exception {
        // Create group and add an entry with "error" in the content
        long groupId = createGroup("Validation Group");
        addEntry(groupId, "This entry has an error in it");
        addEntry(groupId, "This entry is fine");

        // Wait for ENTRY_ADDED events to be processed — the forbidden entry causes a 400
        // from the mock, which is recorded as an error in Edge, but ENTRY_ADDED failures
        // do not change group status (only GROUP_CLOSED drives status transitions)
        await().atMost(TIMEOUT).pollInterval(POLL_INTERVAL).untilAsserted(() -> {
            JsonNode edgeGroup = getEdgeGroup(groupId);
            assertThat(edgeGroup.get("errors").size()).isGreaterThan(0);
        });

        // Verify the error was recorded for the forbidden entry
        JsonNode edgeGroup = getEdgeGroup(groupId);
        boolean hasValidationError = false;
        for (JsonNode error : edgeGroup.get("errors")) {
            if (error.get("message").asText().contains("400")) {
                hasValidationError = true;
                break;
            }
        }
        assertThat(hasValidationError).as("Expected a 400 error from content validation").isTrue();

        // Close the group — GROUP_CLOSED must NOT be affected by content validation
        closeGroup(groupId);

        // GROUP_CLOSED succeeds (no forbidden content check) → group becomes ARCHIVED
        await().atMost(TIMEOUT).pollInterval(POLL_INTERVAL).untilAsserted(() -> {
            JsonNode group = getEdgeGroup(groupId);
            assertThat(group.get("status").asText()).isEqualTo("ARCHIVED");
        });

        // Verify mock history: the forbidden ENTRY_ADDED was rejected, GROUP_CLOSED was accepted
        JsonNode history = getMockHistory();
        long noarkaCount = countHistoryByEndpoint(history, "noarka");
        // 2 ENTRY_ADDED + 1 GROUP_CLOSED = 3 requests to noark-a
        assertThat(noarkaCount).isEqualTo(3);
    }

    // ==================== SCENARIO 12: EXPONENTIAL BACKOFF ====================

    @Test
    @Order(12)
    void exponentialBackoff_failResponsesTwoFiveHundreds_archivedAfterAboutElevenSeconds() throws Exception {
        long groupId = createGroup("Backoff Group");
        addEntry(groupId, "Backoff content");

        // Wait for the ENTRY_ADDED hop to noark-a to land before queuing failures —
        // otherwise the ENTRY_ADDED request would consume the first queued 500.
        await().atMost(TIMEOUT).pollInterval(POLL_INTERVAL).untilAsserted(() -> {
            JsonNode history = getMockHistory();
            assertThat(countHistoryByEndpoint(history, "noarka")).isGreaterThanOrEqualTo(1);
        });

        // Queue 2 failures so the next 2 noark-a requests (both GROUP_CLOSED attempts) return 500;
        // the 3rd attempt finds the queue empty and gets 200.
        // Backoff schedule: after attempt 1 → wait 3s, after attempt 2 → wait 8s, total ≈ 11s.
        configureMockFailResponses("noarka", java.util.List.of(500, 500));

        long startMillis = System.currentTimeMillis();
        closeGroup(groupId);

        // Pump retryDue() in the polling loop to simulate the @Scheduled job (which is
        // disabled in this test class). retryDue() will only actually retry once each
        // group's nextRetryAt has elapsed, so the real backoff timing is exercised.
        await().atMost(Duration.ofSeconds(30)).pollInterval(Duration.ofMillis(500))
                .untilAsserted(() -> {
                    archiveService.retryDue();
                    JsonNode edgeGroup = getEdgeGroup(groupId);
                    assertThat(edgeGroup.get("status").asText()).isEqualTo("ARCHIVED");
                });

        long elapsedMillis = System.currentTimeMillis() - startMillis;
        // Lower bound: must wait at least 3s (attempt 1→2) + 8s (attempt 2→3) ≈ 11s.
        // Allow some slack for poll cadence (-500ms) and CI noise.
        assertThat(elapsedMillis)
                .as("elapsed=%dms — backoff sum 3s+8s should gate archiving to ≥10s", elapsedMillis)
                .isGreaterThanOrEqualTo(10_000L);
        // Upper bound: 11s + a couple extra polls + scheduling jitter
        assertThat(elapsedMillis)
                .as("elapsed=%dms — should not greatly exceed 11s sum", elapsedMillis)
                .isLessThanOrEqualTo(20_000L);

        // Verify retryCount reached 2 (attempt 1 fail + attempt 2 fail + attempt 3 success)
        JsonNode edgeGroup = getEdgeGroup(groupId);
        assertThat(edgeGroup.get("retryCount").asInt()).isEqualTo(2);
    }

    // ==================== HELPER METHODS ====================

    private long createGroup(String name) throws Exception {
        String response = restClient.post()
                .uri("http://localhost:" + logManagerPort + "/api/groups")
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"name\": \"" + name + "\"}")
                .retrieve()
                .body(String.class);
        JsonNode node = objectMapper.readTree(response);
        return node.get("id").asLong();
    }

    private void addEntry(long groupId, String content) {
        restClient.post()
                .uri("http://localhost:" + logManagerPort + "/api/groups/" + groupId + "/entries")
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"content\": \"" + content + "\"}")
                .retrieve()
                .body(String.class);
    }

    private void closeGroup(long groupId) {
        restClient.post()
                .uri("http://localhost:" + logManagerPort + "/api/groups/" + groupId + "/close")
                .retrieve()
                .body(String.class);
    }

    private JsonNode getEdgeGroup(long groupId) throws Exception {
        String response = restClient.get()
                .uri("http://localhost:" + edgePort + "/api/groups/" + groupId)
                .retrieve()
                .body(String.class);
        return objectMapper.readTree(response);
    }

    private JsonNode getMockHistory() throws Exception {
        String response = restClient.get()
                .uri("http://localhost:" + mockPort + "/api/test/history")
                .retrieve()
                .body(String.class);
        return objectMapper.readTree(response);
    }

    private long countHistoryByEndpoint(JsonNode history, String endpoint) {
        long count = 0;
        for (JsonNode entry : history) {
            if (endpoint.equals(entry.get("endpoint").asText())) {
                count++;
            }
        }
        return count;
    }

    private void configureMock(String endpoint, int statusCode, String body) {
        restClient.post()
                .uri("http://localhost:" + mockPort + "/api/test/setup")
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"endpoint\": \"" + endpoint + "\", \"statusCode\": " + statusCode + ", \"body\": \"" + body.replace("\"", "\\\"") + "\"}")
                .retrieve()
                .body(String.class);
    }

    private void configureMockFailResponses(String endpoint, java.util.List<Integer> failResponses) {
        String codes = failResponses.stream().map(String::valueOf).collect(Collectors.joining(","));
        restClient.post()
                .uri("http://localhost:" + mockPort + "/api/test/setup")
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"endpoint\": \"" + endpoint + "\", \"failResponses\": [" + codes + "]}")
                .retrieve()
                .body(String.class);
    }

    private void configureMockWithDelay(String endpoint, int statusCode, long delayMs) {
        restClient.post()
                .uri("http://localhost:" + mockPort + "/api/test/setup")
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"endpoint\": \"" + endpoint + "\", \"statusCode\": " + statusCode + ", \"delayMs\": " + delayMs + "}")
                .retrieve()
                .body(String.class);
    }

    private void triggerEdgeRetry() {
        restClient.post()
                .uri("http://localhost:" + edgePort + "/api/retry")
                .retrieve()
                .body(String.class);
    }
}
