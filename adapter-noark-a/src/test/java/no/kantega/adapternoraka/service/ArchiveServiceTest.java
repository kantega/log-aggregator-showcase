package no.kantega.adapternoraka.service;

import no.kantega.adapternoraka.model.ArchiveRequest;
import no.kantega.adapternoraka.model.ArchiveResult;
import no.kantega.adapternoraka.model.NoarkAPayload;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ArchiveServiceTest {

    @Mock
    private TransformService transformService;

    @Mock
    private NoarkAClient noarkAClient;

    @InjectMocks
    private ArchiveService archiveService;

    @Test
    void archive_success() {
        ArchiveRequest request = new ArchiveRequest("GROUP_CLOSED", 1L, "Group", List.of());
        when(transformService.transform(request)).thenReturn(new NoarkAPayload());
        when(noarkAClient.postArchive(any())).thenReturn("{\"status\":\"ok\"}");

        ArchiveResult result = archiveService.archive(request);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getAdapter()).isEqualTo("noark-a");
    }

    @Test
    void archive_failureFromClient() {
        ArchiveRequest request = new ArchiveRequest("GROUP_CLOSED", 1L, "Group", List.of());
        when(transformService.transform(request)).thenReturn(new NoarkAPayload());
        when(noarkAClient.postArchive(any())).thenThrow(new RuntimeException("Connection refused"));

        ArchiveResult result = archiveService.archive(request);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).isEqualTo("Connection refused");
        assertThat(result.getAdapter()).isEqualTo("noark-a");
    }

    @Test
    void archive_entryAdded_archivesEntry() {
        ArchiveRequest request = new ArchiveRequest("ENTRY_ADDED", 1L, "Group",
                List.of(new ArchiveRequest.LogEntry(10L, "content", "2026-04-03T12:00:00")));
        when(transformService.transform(request)).thenReturn(new NoarkAPayload());
        when(noarkAClient.postArchive(any())).thenReturn("{\"status\":\"ok\"}");

        ArchiveResult result = archiveService.archive(request);

        assertThat(result.isSuccess()).isTrue();
        verify(transformService).transform(request);
        verify(noarkAClient).postArchive(any());
    }

    @Test
    void archive_groupClosed_archivesGroup() {
        ArchiveRequest request = new ArchiveRequest("GROUP_CLOSED", 1L, "Group",
                List.of(new ArchiveRequest.LogEntry(10L, "content", "2026-04-03T12:00:00")));
        when(transformService.transform(request)).thenReturn(new NoarkAPayload());
        when(noarkAClient.postArchive(any())).thenReturn("{\"status\":\"ok\"}");

        ArchiveResult result = archiveService.archive(request);

        assertThat(result.isSuccess()).isTrue();
        verify(transformService).transform(request);
        verify(noarkAClient).postArchive(any());
    }

    @Test
    void archive_failureFromTransform() {
        ArchiveRequest request = new ArchiveRequest("GROUP_CLOSED", 1L, "Group", List.of());
        when(transformService.transform(request)).thenThrow(new RuntimeException("Transform error"));

        ArchiveResult result = archiveService.archive(request);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).isEqualTo("Transform error");
    }
}
