package ${package}.service;

import ${package}.model.ArchiveRequest;
import ${package}.model.${providerName}Payload;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Scaffold tests for TransformService. The skill replaces the body of these
 * tests with real assertions once it has mapped the ${providerName} payload
 * fields from the OpenAPI spec.
 */
class TransformServiceTest {

    private final TransformService transformService = new TransformService();

    @Test
    void transform_returnsPayload() {
        ArchiveRequest request = new ArchiveRequest(
                "GROUP_CLOSED",
                42L,
                "test group",
                List.of(new ArchiveRequest.LogEntry(1L, "hello", "2026-01-01T00:00:00Z"))
        );

        ${providerName}Payload payload = transformService.transform(request);

        assertThat(payload).isNotNull();
        // TODO(skill): Add field-level assertions once the payload shape is filled in.
    }
}
