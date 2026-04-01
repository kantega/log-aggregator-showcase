package no.kantega.logmanager.service;

import no.kantega.logmanager.model.LogEntry;
import no.kantega.logmanager.model.LogGroup;
import no.kantega.logmanager.repository.LogEntryRepository;
import no.kantega.logmanager.repository.LogGroupRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LogManagerServiceTest {

    @Mock
    private LogGroupRepository logGroupRepository;

    @Mock
    private LogEntryRepository logEntryRepository;

    @Mock
    private RabbitMQPublisher rabbitMQPublisher;

    @InjectMocks
    private LogManagerService logManagerService;

    @Test
    void createGroup_success() {
        LogGroup group = LogGroup.builder().id(1L).name("Test Group").status("OPEN").build();
        when(logGroupRepository.save(any(LogGroup.class))).thenReturn(group);

        LogGroup result = logManagerService.createGroup("Test Group");

        assertEquals("Test Group", result.getName());
        assertEquals("OPEN", result.getStatus());
        verify(logGroupRepository).save(any(LogGroup.class));
        verify(rabbitMQPublisher).publishEvent(eq("GROUP_CREATED"), eq(group), isNull());
    }

    @Test
    void addEntry_success() {
        LogGroup group = LogGroup.builder().id(1L).name("Test Group").status("OPEN").build();
        LogEntry entry = LogEntry.builder().id(1L).content("Test entry").group(group).build();

        when(logGroupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(logEntryRepository.save(any(LogEntry.class))).thenReturn(entry);

        LogEntry result = logManagerService.addEntry(1L, "Test entry");

        assertEquals("Test entry", result.getContent());
        verify(logEntryRepository).save(any(LogEntry.class));
        verify(rabbitMQPublisher).publishEvent(eq("ENTRY_ADDED"), eq(group), eq("Test entry"));
    }

    @Test
    void addEntry_closedGroup_throwsException() {
        LogGroup group = LogGroup.builder().id(1L).name("Test Group").status("CLOSED").build();
        when(logGroupRepository.findById(1L)).thenReturn(Optional.of(group));

        assertThrows(ResponseStatusException.class, () -> logManagerService.addEntry(1L, "Test entry"));
        verify(logEntryRepository, never()).save(any());
    }

    @Test
    void closeGroup_success() {
        LogGroup group = LogGroup.builder().id(1L).name("Test Group").status("OPEN").build();
        LogGroup closedGroup = LogGroup.builder().id(1L).name("Test Group").status("CLOSED").build();

        when(logGroupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(logGroupRepository.save(any(LogGroup.class))).thenReturn(closedGroup);

        LogGroup result = logManagerService.closeGroup(1L);

        assertEquals("CLOSED", result.getStatus());
        verify(logGroupRepository).save(any(LogGroup.class));
        verify(rabbitMQPublisher).publishEvent(eq("GROUP_CLOSED"), any(LogGroup.class), isNull());
    }

    @Test
    void closeGroup_alreadyClosed_throwsException() {
        LogGroup group = LogGroup.builder().id(1L).name("Test Group").status("CLOSED").build();
        when(logGroupRepository.findById(1L)).thenReturn(Optional.of(group));

        assertThrows(ResponseStatusException.class, () -> logManagerService.closeGroup(1L));
        verify(logGroupRepository, never()).save(any());
    }

    @Test
    void getGroup_notFound_throwsException() {
        when(logGroupRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> logManagerService.getGroup(1L));
    }
}
