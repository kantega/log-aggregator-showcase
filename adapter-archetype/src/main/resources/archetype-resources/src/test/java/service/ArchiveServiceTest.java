package ${package}.service;

import ${package}.model.ArchiveRequest;
import ${package}.model.ArchiveResult;
import ${package}.model.${providerName}Payload;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ArchiveServiceTest {

    @Mock
    private TransformService transformService;

    @Mock
    private ${providerName}Client ${providerKey}Client;

    @InjectMocks
    private ArchiveService archiveService;

    @Test
    void archive_success() {
        ArchiveRequest request = new ArchiveRequest("GROUP_CLOSED", 1L, "Group", List.of());
        when(transformService.transform(request)).thenReturn(new ${providerName}Payload());
        when(${providerKey}Client.postArchive(any())).thenReturn("{\"status\":\"ok\"}");

        ArchiveResult result = archiveService.archive(request);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getAdapter()).isEqualTo("${providerSlug}");
    }

    @Test
    void archive_failureFromClient() {
        ArchiveRequest request = new ArchiveRequest("GROUP_CLOSED", 1L, "Group", List.of());
        when(transformService.transform(request)).thenReturn(new ${providerName}Payload());
        when(${providerKey}Client.postArchive(any())).thenThrow(new RuntimeException("Connection refused"));

        ArchiveResult result = archiveService.archive(request);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).isEqualTo("Connection refused");
    }

    @Test
    void archive_failureFromTransform() {
        ArchiveRequest request = new ArchiveRequest("GROUP_CLOSED", 1L, "Group", List.of());
        when(transformService.transform(request)).thenThrow(new RuntimeException("Transform error"));

        ArchiveResult result = archiveService.archive(request);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).isEqualTo("Transform error");
        verify(${providerKey}Client, never()).postArchive(any());
    }
}
