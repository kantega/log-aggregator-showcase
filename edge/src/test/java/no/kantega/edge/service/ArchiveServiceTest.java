package no.kantega.edge.service;

import no.kantega.edge.config.AdapterConfig;
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

    // --- notifyAdapters: all adapters called ---

    @Test
    void notifyAdapters_callsAllRegisteredAdapters() {
        ArchiveService service = createService(List.of(
                new AdapterConfig("adapter-a", "http://a:8082"),
                new AdapterConfig("adapter-b", "http://b:8083")));

        ArchiveGroup group = createGroup(1L, "Test");
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(adapterClient.sendToAdapter(any(), any()))
                .thenReturn(new ArchiveResult(true, "ok", "adapter"));

        service.notifyAdapters(group, "ENTRY_ADDED");

        verify(adapterClient).sendToAdapter(eq("http://a:8082"), any());
        verify(adapterClient).sendToAdapter(eq("http://b:8083"), any());
    }

    @Test
    void notifyAdapters_callsAllAdaptersOnGroupClosed() {
        ArchiveService service = createService(List.of(
                new AdapterConfig("adapter-a", "http://a:8082"),
                new AdapterConfig("adapter-b", "http://b:8083")));

        ArchiveGroup group = createGroup(1L, "Test");
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(adapterClient.sendToAdapter(any(), any()))
                .thenReturn(new ArchiveResult(true, "ok", "adapter"));

        service.notifyAdapters(group, "GROUP_CLOSED");

        verify(adapterClient).sendToAdapter(eq("http://a:8082"), any());
        verify(adapterClient).sendToAdapter(eq("http://b:8083"), any());
    }

    @Test
    void notifyAdapters_includesEventTypeInRequest() {
        ArchiveService service = createService(List.of(
                new AdapterConfig("adapter-a", "http://a:8082")));

        ArchiveGroup group = createGroup(1L, "Test");
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(adapterClient.sendToAdapter(any(), any()))
                .thenReturn(new ArchiveResult(true, "ok", "adapter"));

        service.notifyAdapters(group, "ENTRY_ADDED");

        verify(adapterClient).sendToAdapter(eq("http://a:8082"), argThat(req ->
                "ENTRY_ADDED".equals(req.getEventType())));
    }

    // --- GROUP_CLOSED status tracking ---

    @Test
    void notifyAdapters_groupClosed_allSucceed_statusArchived() {
        ArchiveService service = createService(List.of(
                new AdapterConfig("adapter-a", "http://a:8082"),
                new AdapterConfig("adapter-b", "http://b:8083")));

        ArchiveGroup group = createGroup(1L, "Test");
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(adapterClient.sendToAdapter(any(), any()))
                .thenReturn(new ArchiveResult(true, "ok", "adapter"));

        service.notifyAdapters(group, "GROUP_CLOSED");

        assertThat(group.getStatus()).isEqualTo(ArchiveStatus.ARCHIVED);
    }

    @Test
    void notifyAdapters_groupClosed_adapterFails_statusPending() {
        ArchiveService service = createService(List.of(
                new AdapterConfig("adapter-a", "http://a:8082")));

        ArchiveGroup group = createGroup(1L, "Test");
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(adapterClient.sendToAdapter(any(), any()))
                .thenReturn(new ArchiveResult(false, "timeout", "adapter-a"));

        service.notifyAdapters(group, "GROUP_CLOSED");

        assertThat(group.getStatus()).isEqualTo(ArchiveStatus.PENDING);
        assertThat(group.getRetryCount()).isEqualTo(1);
        assertThat(group.getErrors()).hasSize(1);
    }

    @Test
    void notifyAdapters_groupClosed_maxRetries_statusFailed() {
        ArchiveService service = createService(List.of(
                new AdapterConfig("adapter-a", "http://a:8082")));

        ArchiveGroup group = createGroup(1L, "Test");
        group.setRetryCount(2);
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(adapterClient.sendToAdapter(any(), any()))
                .thenReturn(new ArchiveResult(false, "fail", "adapter-a"));

        service.notifyAdapters(group, "GROUP_CLOSED");

        assertThat(group.getStatus()).isEqualTo(ArchiveStatus.FAILED);
        assertThat(group.getRetryCount()).isEqualTo(3);
    }

    // --- ENTRY_ADDED does not change group status ---

    @Test
    void notifyAdapters_entryAdded_doesNotChangeGroupStatus() {
        ArchiveService service = createService(List.of(
                new AdapterConfig("adapter-a", "http://a:8082")));

        ArchiveGroup group = createGroup(1L, "Test");
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(adapterClient.sendToAdapter(any(), any()))
                .thenReturn(new ArchiveResult(true, "ok", "adapter-a"));

        service.notifyAdapters(group, "ENTRY_ADDED");

        assertThat(group.getStatus()).isEqualTo(ArchiveStatus.PENDING);
    }

    @Test
    void notifyAdapters_entryAdded_adapterFails_recordsErrorButKeepsStatus() {
        ArchiveService service = createService(List.of(
                new AdapterConfig("adapter-a", "http://a:8082")));

        ArchiveGroup group = createGroup(1L, "Test");
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(adapterClient.sendToAdapter(any(), any()))
                .thenReturn(new ArchiveResult(false, "timeout", "adapter-a"));

        service.notifyAdapters(group, "ENTRY_ADDED");

        assertThat(group.getStatus()).isEqualTo(ArchiveStatus.PENDING);
        assertThat(group.getErrors()).hasSize(1);
    }

    // --- retry ---

    @Test
    void retryFailed_retriesPendingGroupsWithRetryCount() {
        ArchiveService service = createService(List.of(
                new AdapterConfig("adapter-a", "http://a:8082")));

        ArchiveGroup retryable = createGroup(1L, "Retry");
        retryable.setRetryCount(1);
        ArchiveGroup fresh = createGroup(2L, "Fresh");
        when(repository.findByStatus(ArchiveStatus.PENDING)).thenReturn(List.of(retryable, fresh));
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(adapterClient.sendToAdapter(any(), any()))
                .thenReturn(new ArchiveResult(true, "ok", "adapter-a"));

        service.retryFailed();

        verify(adapterClient, times(1)).sendToAdapter(any(), any());
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
