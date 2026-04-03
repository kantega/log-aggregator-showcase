package no.kantega.edge.service;

import no.kantega.edge.config.AdapterConfig;
import no.kantega.edge.config.AdapterConfig.TriggerType;
import no.kantega.edge.config.AdaptersProperties;
import no.kantega.edge.model.ArchiveGroup;
import no.kantega.edge.model.ArchiveGroup.ArchiveError;
import no.kantega.edge.model.ArchiveGroup.ArchiveStatus;
import no.kantega.edge.model.ArchiveGroup.LogEntryData;
import no.kantega.edge.model.ArchiveRequest;
import no.kantega.edge.model.ArchiveResult;
import no.kantega.edge.repository.ArchiveGroupRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ArchiveService {

    private static final Logger log = LoggerFactory.getLogger(ArchiveService.class);
    private static final int MAX_RETRIES = 3;

    private final ArchiveGroupRepository repository;
    private final AdapterClient adapterClient;
    private final List<AdapterConfig> adapters;

    public ArchiveService(
            ArchiveGroupRepository repository,
            AdapterClient adapterClient,
            AdaptersProperties adaptersProperties) {
        this.repository = repository;
        this.adapterClient = adapterClient;
        this.adapters = adaptersProperties.getAdapters();
    }

    public void archiveEntry(ArchiveGroup group, LogEntryData entry) {
        List<AdapterConfig> entryAdapters = adapters.stream()
                .filter(a -> a.getTrigger() == TriggerType.ON_ENTRY)
                .collect(Collectors.toList());

        ArchiveRequest request = new ArchiveRequest(
                group.getGroupId(),
                group.getName(),
                List.of(new ArchiveRequest.LogEntry(entry.getEntryId(), entry.getContent(), entry.getTimestamp()))
        );

        for (AdapterConfig adapter : entryAdapters) {
            entry.getAdapterStatuses().put(adapter.getName(), ArchiveStatus.IN_PROGRESS);
            repository.save(group);

            ArchiveResult result = adapterClient.sendToAdapter(adapter.getUrl(), request);

            if (result.isSuccess()) {
                entry.getAdapterStatuses().put(adapter.getName(), ArchiveStatus.ARCHIVED);
                log.info("Entry {} archived to {} for group {}", entry.getEntryId(), adapter.getName(), group.getGroupId());
            } else {
                entry.getAdapterStatuses().put(adapter.getName(), ArchiveStatus.FAILED);
                group.getErrors().add(new ArchiveError(adapter.getName(), result.getMessage(), Instant.now()));
                log.error("Failed to archive entry {} to {} for group {}: {}",
                        entry.getEntryId(), adapter.getName(), group.getGroupId(), result.getMessage());
            }
        }

        group.setUpdatedAt(Instant.now());
        repository.save(group);
    }

    public void archiveGroup(ArchiveGroup group) {
        List<AdapterConfig> groupAdapters = adapters.stream()
                .filter(a -> a.getTrigger() == TriggerType.ON_GROUP_CLOSE)
                .collect(Collectors.toList());

        group.setStatus(ArchiveStatus.IN_PROGRESS);
        group.setUpdatedAt(Instant.now());
        repository.save(group);

        ArchiveRequest request = toArchiveRequest(group);

        List<ArchiveError> newErrors = new ArrayList<>();
        for (AdapterConfig adapter : groupAdapters) {
            ArchiveResult result = adapterClient.sendToAdapter(adapter.getUrl(), request);
            if (!result.isSuccess()) {
                newErrors.add(new ArchiveError(adapter.getName(), result.getMessage(), Instant.now()));
            }
        }

        if (newErrors.isEmpty()) {
            group.setStatus(ArchiveStatus.ARCHIVED);
            log.info("Group {} archived successfully", group.getGroupId());
        } else {
            group.getErrors().addAll(newErrors);
            group.setRetryCount(group.getRetryCount() + 1);
            if (group.getRetryCount() >= MAX_RETRIES) {
                group.setStatus(ArchiveStatus.FAILED);
                log.error("Group {} failed after {} retries", group.getGroupId(), MAX_RETRIES);
            } else {
                group.setStatus(ArchiveStatus.PENDING);
                log.warn("Group {} archive attempt failed, will retry ({}/{})",
                        group.getGroupId(), group.getRetryCount(), MAX_RETRIES);
            }
        }

        group.setUpdatedAt(Instant.now());
        repository.save(group);
    }

    public void retryGroup(ArchiveGroup group) {
        log.info("Manual retry triggered for group {}", group.getGroupId());
        archiveGroup(group);
    }

    public void retryFailed() {
        List<ArchiveGroup> pending = repository.findByStatus(ArchiveStatus.PENDING);
        List<ArchiveGroup> retryable = pending.stream()
                .filter(g -> g.getRetryCount() > 0 && g.getRetryCount() < MAX_RETRIES)
                .collect(Collectors.toList());

        for (ArchiveGroup group : retryable) {
            log.info("Retrying archive for group {} (attempt {}/{})",
                    group.getGroupId(), group.getRetryCount() + 1, MAX_RETRIES);
            archiveGroup(group);
        }
    }

    private ArchiveRequest toArchiveRequest(ArchiveGroup group) {
        List<ArchiveRequest.LogEntry> entries = group.getEntries().stream()
                .map(e -> new ArchiveRequest.LogEntry(e.getEntryId(), e.getContent(), e.getTimestamp()))
                .collect(Collectors.toList());
        return new ArchiveRequest(group.getGroupId(), group.getName(), entries);
    }
}
