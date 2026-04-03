package no.kantega.edge.service;

import no.kantega.edge.config.AdapterConfig;
import no.kantega.edge.config.AdapterConfig.TriggerType;
import no.kantega.edge.config.AdaptersProperties;
import no.kantega.edge.model.ArchiveGroup;
import no.kantega.edge.model.ArchiveGroup.ArchiveStatus;
import no.kantega.edge.model.ArchiveResult;
import no.kantega.edge.repository.ArchiveGroupRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

    private ArchiveService createService(List<AdapterConfig> adapters) {
        AdaptersProperties props = new AdaptersProperties();
        props.setAdapters(adapters);
        return new ArchiveService(repository, adapterClient, props);
    }

    private static AdapterConfig entryAdapter(String name, String url) {
        return new AdapterConfig(name, url, TriggerType.ON_ENTRY);
    }

    private static AdapterConfig groupAdapter(String name, String url) {
        return new AdapterConfig(name, url, TriggerType.ON_GROUP_CLOSE);
    }

    // --- archiveGroup tests ---

    @Test
    void archiveGroup_allAdaptersSucceed_statusArchived() {
        ArchiveService service = createService(List.of(
                groupAdapter("group-adapter", "http://group:8083")));

        ArchiveGroup group = createGroup(1L, "Test");
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(adapterClient.sendToAdapter(eq("http://group:8083"), any()))
                .thenReturn(new ArchiveResult(true, "ok", "group-adapter"));

        service.archiveGroup(group);

        assertThat(group.getStatus()).isEqualTo(ArchiveStatus.ARCHIVED);
        assertThat(group.getErrors()).isEmpty();
    }

    @Test
    void archiveGroup_adapterFails_statusPendingForRetry() {
        ArchiveService service = createService(List.of(
                groupAdapter("group-adapter", "http://group:8083")));

        ArchiveGroup group = createGroup(1L, "Test");
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(adapterClient.sendToAdapter(eq("http://group:8083"), any()))
                .thenReturn(new ArchiveResult(false, "Connection refused", "group-adapter"));

        service.archiveGroup(group);

        assertThat(group.getStatus()).isEqualTo(ArchiveStatus.PENDING);
        assertThat(group.getRetryCount()).isEqualTo(1);
        assertThat(group.getErrors()).hasSize(1);
        assertThat(group.getErrors().get(0).getAdapter()).isEqualTo("group-adapter");
    }

    @Test
    void archiveGroup_maxRetriesReached_statusFailed() {
        ArchiveService service = createService(List.of(
                groupAdapter("group-adapter", "http://group:8083")));

        ArchiveGroup group = createGroup(1L, "Test");
        group.setRetryCount(2);
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(adapterClient.sendToAdapter(eq("http://group:8083"), any()))
                .thenReturn(new ArchiveResult(false, "fail", "group-adapter"));

        service.archiveGroup(group);

        assertThat(group.getStatus()).isEqualTo(ArchiveStatus.FAILED);
        assertThat(group.getRetryCount()).isEqualTo(3);
    }

    @Test
    void archiveGroup_setsInProgressBeforeArchiving() {
        ArchiveService service = createService(List.of(
                groupAdapter("group-adapter", "http://group:8083")));

        ArchiveGroup group = createGroup(1L, "Test");
        List<ArchiveStatus> statusesAtSave = new ArrayList<>();
        when(repository.save(any(ArchiveGroup.class))).thenAnswer(i -> {
            ArchiveGroup g = i.getArgument(0);
            statusesAtSave.add(g.getStatus());
            return g;
        });
        when(adapterClient.sendToAdapter(any(), any()))
                .thenReturn(new ArchiveResult(true, "ok", "group-adapter"));

        service.archiveGroup(group);

        assertThat(statusesAtSave).hasSize(2);
        assertThat(statusesAtSave.get(0)).isEqualTo(ArchiveStatus.IN_PROGRESS);
        assertThat(statusesAtSave.get(1)).isEqualTo(ArchiveStatus.ARCHIVED);
    }

    @Test
    void archiveGroup_doesNotCallEntryAdapters() {
        ArchiveService service = createService(List.of(
                entryAdapter("entry-adapter", "http://entry:8082"),
                groupAdapter("group-adapter", "http://group:8083")));

        ArchiveGroup group = createGroup(1L, "Test");
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(adapterClient.sendToAdapter(eq("http://group:8083"), any()))
                .thenReturn(new ArchiveResult(true, "ok", "group-adapter"));

        service.archiveGroup(group);

        verify(adapterClient, never()).sendToAdapter(eq("http://entry:8082"), any());
        verify(adapterClient).sendToAdapter(eq("http://group:8083"), any());
    }

    // --- archiveEntry tests ---

    @Test
    void archiveEntry_success_setsAdapterStatusArchived() {
        ArchiveService service = createService(List.of(
                entryAdapter("entry-adapter", "http://entry:8082"),
                groupAdapter("group-adapter", "http://group:8083")));

        ArchiveGroup group = createGroup(1L, "Test");
        ArchiveGroup.LogEntryData entry = new ArchiveGroup.LogEntryData(10L, "content", "2026-04-03T12:00:00");
        group.getEntries().add(entry);

        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(adapterClient.sendToAdapter(eq("http://entry:8082"), any()))
                .thenReturn(new ArchiveResult(true, "ok", "entry-adapter"));

        service.archiveEntry(group, entry);

        assertThat(entry.getAdapterStatuses().get("entry-adapter")).isEqualTo(ArchiveStatus.ARCHIVED);
        assertThat(group.getErrors()).isEmpty();
        verify(adapterClient, never()).sendToAdapter(eq("http://group:8083"), any());
    }

    @Test
    void archiveEntry_failure_setsAdapterStatusFailed() {
        ArchiveService service = createService(List.of(
                entryAdapter("entry-adapter", "http://entry:8082")));

        ArchiveGroup group = createGroup(1L, "Test");
        ArchiveGroup.LogEntryData entry = new ArchiveGroup.LogEntryData(10L, "content", "2026-04-03T12:00:00");
        group.getEntries().add(entry);

        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(adapterClient.sendToAdapter(eq("http://entry:8082"), any()))
                .thenReturn(new ArchiveResult(false, "timeout", "entry-adapter"));

        service.archiveEntry(group, entry);

        assertThat(entry.getAdapterStatuses().get("entry-adapter")).isEqualTo(ArchiveStatus.FAILED);
        assertThat(group.getErrors()).hasSize(1);
        assertThat(group.getErrors().get(0).getAdapter()).isEqualTo("entry-adapter");
    }

    @Test
    void archiveEntry_multipleAdapters_callsAll() {
        ArchiveService service = createService(List.of(
                entryAdapter("adapter-x", "http://x:8082"),
                entryAdapter("adapter-y", "http://y:8085")));

        ArchiveGroup group = createGroup(1L, "Test");
        ArchiveGroup.LogEntryData entry = new ArchiveGroup.LogEntryData(10L, "content", "2026-04-03T12:00:00");
        group.getEntries().add(entry);

        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(adapterClient.sendToAdapter(eq("http://x:8082"), any()))
                .thenReturn(new ArchiveResult(true, "ok", "adapter-x"));
        when(adapterClient.sendToAdapter(eq("http://y:8085"), any()))
                .thenReturn(new ArchiveResult(true, "ok", "adapter-y"));

        service.archiveEntry(group, entry);

        assertThat(entry.getAdapterStatuses().get("adapter-x")).isEqualTo(ArchiveStatus.ARCHIVED);
        assertThat(entry.getAdapterStatuses().get("adapter-y")).isEqualTo(ArchiveStatus.ARCHIVED);
    }

    // --- retry tests ---

    @Test
    void retryFailed_retriesPendingGroupsWithRetryCount() {
        ArchiveService service = createService(List.of(
                groupAdapter("group-adapter", "http://group:8083")));

        ArchiveGroup retryable = createGroup(1L, "Retry");
        retryable.setRetryCount(1);
        ArchiveGroup fresh = createGroup(2L, "Fresh");
        when(repository.findByStatus(ArchiveStatus.PENDING)).thenReturn(List.of(retryable, fresh));
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(adapterClient.sendToAdapter(any(), any()))
                .thenReturn(new ArchiveResult(true, "ok", "group-adapter"));

        service.retryFailed();

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
