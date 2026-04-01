package no.kantega.edge.service;

import no.kantega.edge.model.ArchiveGroup;
import no.kantega.edge.model.ArchiveGroup.ArchiveStatus;
import no.kantega.edge.model.ArchiveResult;
import no.kantega.edge.repository.ArchiveGroupRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ArchiveServiceTest {

    @Mock
    private ArchiveGroupRepository repository;

    @Mock
    private AdapterClient adapterClient;

    @Test
    void archiveGroup_bothAdaptersSucceed_statusArchived() {
        ArchiveService service = new ArchiveService(repository, adapterClient,
                "http://a:8082", "http://b:8083");

        ArchiveGroup group = createGroup(1L, "Test");
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(adapterClient.sendToAdapter(eq("http://a:8082"), any()))
                .thenReturn(new ArchiveResult(true, "ok", "noark-a"));
        when(adapterClient.sendToAdapter(eq("http://b:8083"), any()))
                .thenReturn(new ArchiveResult(true, "ok", "noark-b"));

        service.archiveGroup(group);

        assertThat(group.getStatus()).isEqualTo(ArchiveStatus.ARCHIVED);
        assertThat(group.getErrors()).isEmpty();
        verify(repository, times(2)).save(group);
    }

    @Test
    void archiveGroup_adapterAFails_statusPendingForRetry() {
        ArchiveService service = new ArchiveService(repository, adapterClient,
                "http://a:8082", "http://b:8083");

        ArchiveGroup group = createGroup(1L, "Test");
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(adapterClient.sendToAdapter(eq("http://a:8082"), any()))
                .thenReturn(new ArchiveResult(false, "Connection refused", "noark-a"));
        when(adapterClient.sendToAdapter(eq("http://b:8083"), any()))
                .thenReturn(new ArchiveResult(true, "ok", "noark-b"));

        service.archiveGroup(group);

        assertThat(group.getStatus()).isEqualTo(ArchiveStatus.PENDING);
        assertThat(group.getRetryCount()).isEqualTo(1);
        assertThat(group.getErrors()).hasSize(1);
        assertThat(group.getErrors().get(0).getAdapter()).isEqualTo("noark-a");
    }

    @Test
    void archiveGroup_maxRetriesReached_statusFailed() {
        ArchiveService service = new ArchiveService(repository, adapterClient,
                "http://a:8082", "http://b:8083");

        ArchiveGroup group = createGroup(1L, "Test");
        group.setRetryCount(2); // already retried twice
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(adapterClient.sendToAdapter(eq("http://a:8082"), any()))
                .thenReturn(new ArchiveResult(false, "fail", "noark-a"));
        when(adapterClient.sendToAdapter(eq("http://b:8083"), any()))
                .thenReturn(new ArchiveResult(false, "fail", "noark-b"));

        service.archiveGroup(group);

        assertThat(group.getStatus()).isEqualTo(ArchiveStatus.FAILED);
        assertThat(group.getRetryCount()).isEqualTo(3);
    }

    @Test
    void archiveGroup_setsInProgressBeforeArchiving() {
        ArchiveService service = new ArchiveService(repository, adapterClient,
                "http://a:8082", "http://b:8083");

        ArchiveGroup group = createGroup(1L, "Test");
        // Capture the status at each save call (object is mutated between saves)
        List<ArchiveStatus> statusesAtSave = new ArrayList<>();
        when(repository.save(any(ArchiveGroup.class))).thenAnswer(i -> {
            ArchiveGroup g = i.getArgument(0);
            statusesAtSave.add(g.getStatus());
            return g;
        });
        when(adapterClient.sendToAdapter(any(), any()))
                .thenReturn(new ArchiveResult(true, "ok", "noark-a"));

        service.archiveGroup(group);

        assertThat(statusesAtSave).hasSize(2);
        assertThat(statusesAtSave.get(0)).isEqualTo(ArchiveStatus.IN_PROGRESS);
        assertThat(statusesAtSave.get(1)).isEqualTo(ArchiveStatus.ARCHIVED);
    }

    @Test
    void retryFailed_retriesPendingGroupsWithRetryCount() {
        ArchiveService service = new ArchiveService(repository, adapterClient,
                "http://a:8082", "http://b:8083");

        ArchiveGroup retryable = createGroup(1L, "Retry");
        retryable.setRetryCount(1);
        ArchiveGroup fresh = createGroup(2L, "Fresh"); // retryCount=0, not retryable
        when(repository.findByStatus(ArchiveStatus.PENDING)).thenReturn(List.of(retryable, fresh));
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(adapterClient.sendToAdapter(any(), any()))
                .thenReturn(new ArchiveResult(true, "ok", "noark-a"));

        service.retryFailed();

        // Only retryable group should have been archived (2 saves per archiveGroup call)
        verify(repository, times(2)).save(any());
    }

    private ArchiveGroup createGroup(Long groupId, String name) {
        ArchiveGroup group = new ArchiveGroup();
        group.setGroupId(groupId);
        group.setName(name);
        group.setStatus(ArchiveStatus.PENDING);
        group.setEntries(new ArrayList<>());
        group.setErrors(new ArrayList<>());
        group.setCreatedAt(Instant.now());
        group.setUpdatedAt(Instant.now());
        return group;
    }
}
