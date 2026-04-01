package no.kantega.logmanager.service;

import no.kantega.logmanager.model.LogEntry;
import no.kantega.logmanager.model.LogGroup;
import no.kantega.logmanager.repository.LogEntryRepository;
import no.kantega.logmanager.repository.LogGroupRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@Transactional
public class LogManagerService {

    private final LogGroupRepository logGroupRepository;
    private final LogEntryRepository logEntryRepository;
    private final RabbitMQPublisher rabbitMQPublisher;

    public LogManagerService(LogGroupRepository logGroupRepository,
                             LogEntryRepository logEntryRepository,
                             RabbitMQPublisher rabbitMQPublisher) {
        this.logGroupRepository = logGroupRepository;
        this.logEntryRepository = logEntryRepository;
        this.rabbitMQPublisher = rabbitMQPublisher;
    }

    public LogGroup createGroup(String name) {
        LogGroup group = LogGroup.builder()
                .name(name)
                .build();
        group = logGroupRepository.save(group);
        rabbitMQPublisher.publishEvent("GROUP_CREATED", group, null, null);
        return group;
    }

    public List<LogGroup> getAllGroups() {
        return logGroupRepository.findAllByOrderByCreatedAtDesc();
    }

    public LogGroup getGroup(Long id) {
        return logGroupRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found"));
    }

    public List<LogEntry> getEntriesForGroup(Long groupId) {
        return logEntryRepository.findByGroupIdOrderByCreatedAtAsc(groupId);
    }

    public LogEntry addEntry(Long groupId, String content) {
        LogGroup group = getGroup(groupId);
        if ("CLOSED".equals(group.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Group is closed");
        }
        LogEntry entry = LogEntry.builder()
                .content(content)
                .group(group)
                .build();
        entry = logEntryRepository.save(entry);
        rabbitMQPublisher.publishEvent("ENTRY_ADDED", group, content, entry.getId());
        return entry;
    }

    public LogGroup closeGroup(Long id) {
        LogGroup group = getGroup(id);
        if ("CLOSED".equals(group.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Group is already closed");
        }
        group.setStatus("CLOSED");
        group = logGroupRepository.save(group);
        rabbitMQPublisher.publishEvent("GROUP_CLOSED", group, null, null);
        return group;
    }
}
