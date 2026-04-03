package no.kantega.adapternorakb.service;

import no.kantega.adapternorakb.model.ArchiveRequest;
import no.kantega.adapternorakb.model.ArchiveResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ArchiveServiceTest {

    @Mock
    private ZipService zipService;

    @Mock
    private NoarkBClient noarkBClient;

    @InjectMocks
    private ArchiveService archiveService;

    @Test
    void archive_success() throws IOException {
        ArchiveRequest request = new ArchiveRequest("GROUP_CLOSED", 1L, "Group", List.of());
        when(zipService.createZip(request)).thenReturn(new byte[]{1, 2, 3});
        when(noarkBClient.postArchive(any())).thenReturn("{\"ok\":true}");

        ArchiveResult result = archiveService.archive(request);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getAdapter()).isEqualTo("noark-b");
    }

    @Test
    void archive_failureFromZip() throws IOException {
        ArchiveRequest request = new ArchiveRequest("GROUP_CLOSED", 1L, "Group", List.of());
        when(zipService.createZip(request)).thenThrow(new IOException("ZIP error"));

        ArchiveResult result = archiveService.archive(request);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).isEqualTo("ZIP error");
    }

    @Test
    void archive_entryAdded_skipsArchiving() {
        ArchiveRequest request = new ArchiveRequest("ENTRY_ADDED", 1L, "Group",
                List.of(new ArchiveRequest.LogEntry(10L, "content", "2026-04-03T12:00:00")));

        ArchiveResult result = archiveService.archive(request);

        assertThat(result.isSuccess()).isTrue();
        verifyNoInteractions(zipService, noarkBClient);
    }

    @Test
    void archive_groupClosed_createsZipAndPosts() throws IOException {
        ArchiveRequest request = new ArchiveRequest("GROUP_CLOSED", 1L, "Group",
                List.of(new ArchiveRequest.LogEntry(10L, "content", "2026-04-03T12:00:00")));
        when(zipService.createZip(request)).thenReturn(new byte[]{1, 2, 3});
        when(noarkBClient.postArchive(any())).thenReturn("{\"ok\":true}");

        ArchiveResult result = archiveService.archive(request);

        assertThat(result.isSuccess()).isTrue();
        verify(zipService).createZip(request);
        verify(noarkBClient).postArchive(any());
    }

    @Test
    void archive_failureFromClient() throws IOException {
        ArchiveRequest request = new ArchiveRequest("GROUP_CLOSED", 1L, "Group", List.of());
        when(zipService.createZip(request)).thenReturn(new byte[]{1, 2, 3});
        when(noarkBClient.postArchive(any())).thenThrow(new RuntimeException("Connection refused"));

        ArchiveResult result = archiveService.archive(request);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).isEqualTo("Connection refused");
    }
}
