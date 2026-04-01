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
import static org.mockito.Mockito.when;

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
        ArchiveRequest request = new ArchiveRequest(1L, "Group", List.of());
        when(zipService.createZip(request)).thenReturn(new byte[]{1, 2, 3});
        when(noarkBClient.postArchive(any())).thenReturn("{\"ok\":true}");

        ArchiveResult result = archiveService.archive(request);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getAdapter()).isEqualTo("noark-b");
    }

    @Test
    void archive_failureFromZip() throws IOException {
        ArchiveRequest request = new ArchiveRequest(1L, "Group", List.of());
        when(zipService.createZip(request)).thenThrow(new IOException("ZIP error"));

        ArchiveResult result = archiveService.archive(request);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).isEqualTo("ZIP error");
    }

    @Test
    void archive_failureFromClient() throws IOException {
        ArchiveRequest request = new ArchiveRequest(1L, "Group", List.of());
        when(zipService.createZip(request)).thenReturn(new byte[]{1, 2, 3});
        when(noarkBClient.postArchive(any())).thenThrow(new RuntimeException("Connection refused"));

        ArchiveResult result = archiveService.archive(request);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).isEqualTo("Connection refused");
    }
}
