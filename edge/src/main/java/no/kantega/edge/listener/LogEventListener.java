package no.kantega.edge.listener;

import no.kantega.edge.config.RabbitConfig;
import no.kantega.edge.model.ArchiveGroup;
import no.kantega.edge.model.ArchiveGroup.ArchiveStatus;
import no.kantega.edge.model.ArchiveGroup.LogEntryData;
import no.kantega.edge.model.LogEvent;
import no.kantega.edge.repository.ArchiveGroupRepository;
import no.kantega.edge.service.ArchiveService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;

@Component
public class LogEventListener {

    private static final Logger log = LoggerFactory.getLogger(LogEventListener.class);

    private final ArchiveGroupRepository repository;
    private final ArchiveService archiveService;

    public LogEventListener(ArchiveGroupRepository repository, ArchiveService archiveService) {
        this.repository = repository;
        this.archiveService = archiveService;
    }

    @RabbitListener(queues = RabbitConfig.QUEUE)
    public void handleEvent(LogEvent event) {
        log.info("Received event: {} for group {}", event.getEvent(), event.getGroupId());

        switch (event.getEvent()) {
            case "GROUP_CREATED" -> handleGroupCreated(event);
            case "ENTRY_ADDED" -> handleEntryAdded(event);
            case "GROUP_CLOSED" -> handleGroupClosed(event);
            default -> log.warn("Unknown event type: {}", event.getEvent());
        }
    }

    private void handleGroupCreated(LogEvent event) {
        ArchiveGroup group = new ArchiveGroup();
        group.setGroupId(event.getGroupId());
        group.setName(event.getName());
        group.setStatus(ArchiveStatus.PENDING);
        group.setEntries(new ArrayList<>());
        group.setErrors(new ArrayList<>());
        group.setCreatedAt(Instant.now());
        group.setUpdatedAt(Instant.now());
        repository.save(group);
        log.info("Created archive group {} ({})", event.getGroupId(), event.getName());
    }

    private void handleEntryAdded(LogEvent event) {
        repository.findByGroupId(event.getGroupId()).ifPresentOrElse(group -> {
            LogEntryData entry = new LogEntryData(
                    event.getEntryId(), event.getContent(), event.getTimestamp());
            group.getEntries().add(entry);
            group.setUpdatedAt(Instant.now());
            repository.save(group);
            log.info("Added entry {} to group {}", event.getEntryId(), event.getGroupId());

            archiveService.archiveEntry(group, entry);
        }, () -> log.warn("Group {} not found for ENTRY_ADDED event", event.getGroupId()));
    }

    private void handleGroupClosed(LogEvent event) {
        repository.findByGroupId(event.getGroupId()).ifPresentOrElse(group -> {
            log.info("Group {} closed, triggering archive", event.getGroupId());
            archiveService.archiveGroup(group);
        }, () -> log.warn("Group {} not found for GROUP_CLOSED event", event.getGroupId()));
    }
}
