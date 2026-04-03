package no.kantega.edge.listener;

import no.kantega.edge.model.ArchiveGroup;
import no.kantega.edge.model.LogEvent;
import no.kantega.edge.repository.ArchiveGroupRepository;
import no.kantega.edge.service.ArchiveService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LogEventListenerTest {

    @Mock
    private ArchiveGroupRepository repository;

    @Mock
    private ArchiveService archiveService;

    @InjectMocks
    private LogEventListener listener;

    @Test
    void handleEvent_groupCreated_savesNewGroup() {
        LogEvent event = new LogEvent("GROUP_CREATED", 1L, "My Group", null, null, "2024-01-01T00:00:00Z");

        listener.handleEvent(event);

        ArgumentCaptor<ArchiveGroup> captor = ArgumentCaptor.forClass(ArchiveGroup.class);
        verify(repository).save(captor.capture());

        ArchiveGroup saved = captor.getValue();
        assertThat(saved.getGroupId()).isEqualTo(1L);
        assertThat(saved.getName()).isEqualTo("My Group");
        assertThat(saved.getStatus()).isEqualTo(ArchiveGroup.ArchiveStatus.PENDING);
        assertThat(saved.getEntries()).isEmpty();
    }

    @Test
    void handleEvent_entryAdded_addsToExistingGroupAndNotifiesAdapters() {
        ArchiveGroup group = new ArchiveGroup();
        group.setGroupId(1L);
        group.setEntries(new java.util.ArrayList<>());
        when(repository.findByGroupId(1L)).thenReturn(Optional.of(group));

        LogEvent event = new LogEvent("ENTRY_ADDED", 1L, null, 10L, "log content", "2024-01-01T00:00:00Z");
        listener.handleEvent(event);

        verify(repository).save(group);
        assertThat(group.getEntries()).hasSize(1);
        assertThat(group.getEntries().get(0).getContent()).isEqualTo("log content");
        assertThat(group.getEntries().get(0).getEntryId()).isEqualTo(10L);
        verify(archiveService).notifyAdapters(group, "ENTRY_ADDED");
    }

    @Test
    void handleEvent_entryAdded_groupNotFound_doesNotThrow() {
        when(repository.findByGroupId(99L)).thenReturn(Optional.empty());

        LogEvent event = new LogEvent("ENTRY_ADDED", 99L, null, 1L, "content", "2024-01-01T00:00:00Z");
        listener.handleEvent(event);

        verify(repository, never()).save(any());
    }

    @Test
    void handleEvent_groupClosed_triggersArchive() {
        ArchiveGroup group = new ArchiveGroup();
        group.setGroupId(1L);
        when(repository.findByGroupId(1L)).thenReturn(Optional.of(group));

        LogEvent event = new LogEvent("GROUP_CLOSED", 1L, null, null, null, "2024-01-01T00:00:00Z");
        listener.handleEvent(event);

        verify(archiveService).notifyAdapters(group, "GROUP_CLOSED");
    }

    @Test
    void handleEvent_groupClosed_groupNotFound_doesNotThrow() {
        when(repository.findByGroupId(99L)).thenReturn(Optional.empty());

        LogEvent event = new LogEvent("GROUP_CLOSED", 99L, null, null, null, "2024-01-01T00:00:00Z");
        listener.handleEvent(event);

        verify(archiveService, never()).notifyAdapters(any(), any());
    }

    @Test
    void handleEvent_unknownEvent_doesNotThrow() {
        LogEvent event = new LogEvent("UNKNOWN", 1L, null, null, null, "2024-01-01T00:00:00Z");
        listener.handleEvent(event);

        verifyNoInteractions(repository, archiveService);
    }
}
