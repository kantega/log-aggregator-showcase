package no.kantega.adapternoraka.service;

import no.kantega.adapternoraka.model.ArchiveRequest;
import no.kantega.adapternoraka.model.NoarkAPayload;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TransformServiceTest {

    private final TransformService transformService = new TransformService();

    @Test
    void transform_mapsGroupFields() {
        ArchiveRequest request = new ArchiveRequest("GROUP_CLOSED", 1L, "Test Group", List.of(
                new ArchiveRequest.LogEntry(10L, "entry content", "2024-01-01T00:00:00Z")
        ));

        NoarkAPayload payload = transformService.transform(request);

        assertThat(payload.getTitle()).isEqualTo("Test Group");
        assertThat(payload.getDescription()).contains("Log group 1");
        assertThat(payload.getArchiveDate()).isNotNull();
    }

    @Test
    void transform_mapsEventType() {
        ArchiveRequest entryRequest = new ArchiveRequest("ENTRY_ADDED", 1L, "Group", List.of(
                new ArchiveRequest.LogEntry(10L, "content", "2024-01-01T00:00:00Z")
        ));
        assertThat(transformService.transform(entryRequest).getEventType()).isEqualTo("ENTRY_ADDED");

        ArchiveRequest closedRequest = new ArchiveRequest("GROUP_CLOSED", 1L, "Group", List.of());
        assertThat(transformService.transform(closedRequest).getEventType()).isEqualTo("GROUP_CLOSED");
    }

    @Test
    void transform_mapsEntriesToDocuments() {
        ArchiveRequest request = new ArchiveRequest("GROUP_CLOSED", 1L, "Group", List.of(
                new ArchiveRequest.LogEntry(10L, "first", "2024-01-01T00:00:00Z"),
                new ArchiveRequest.LogEntry(20L, "second", "2024-01-02T00:00:00Z")
        ));

        NoarkAPayload payload = transformService.transform(request);

        assertThat(payload.getDocuments()).hasSize(2);
        assertThat(payload.getDocuments().get(0).getDocumentId()).isEqualTo("entry-10");
        assertThat(payload.getDocuments().get(0).getContent()).isEqualTo("first");
        assertThat(payload.getDocuments().get(1).getDocumentId()).isEqualTo("entry-20");
        assertThat(payload.getDocuments().get(1).getContent()).isEqualTo("second");
    }

    @Test
    void transform_emptyEntries() {
        ArchiveRequest request = new ArchiveRequest("GROUP_CLOSED", 1L, "Empty Group", List.of());
        NoarkAPayload payload = transformService.transform(request);

        assertThat(payload.getDocuments()).isEmpty();
        assertThat(payload.getTitle()).isEqualTo("Empty Group");
    }
}
