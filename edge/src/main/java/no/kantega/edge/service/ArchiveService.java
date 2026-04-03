package no.kantega.edge.service;

import no.kantega.edge.config.AdapterConfig;
import no.kantega.edge.config.AdaptersProperties;
import no.kantega.edge.model.ArchiveGroup;
import no.kantega.edge.model.ArchiveGroup.ArchiveError;
import no.kantega.edge.model.ArchiveGroup.ArchiveEvent;
import no.kantega.edge.model.ArchiveGroup.ArchiveStatus;
import no.kantega.edge.model.ArchiveGroup.EventStatus;
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

    public void notifyAdapters(ArchiveGroup group, String eventType) {
        ArchiveRequest request = toArchiveRequest(group, eventType);

        List<ArchiveError> newErrors = new ArrayList<>();
        for (AdapterConfig adapter : adapters) {
            ArchiveResult result = adapterClient.sendToAdapter(adapter.getUrl(), request);
            if (!result.isSuccess()) {
                newErrors.add(new ArchiveError(adapter.getName(), eventType, result.getMessage(), Instant.now()));
                group.getArchiveEvents().add(new ArchiveEvent(eventType, adapter.getName(), EventStatus.FAILED, result.getMessage(), Instant.now()));
                log.error("Adapter {} failed for group {} on {}: {}",
                        adapter.getName(), group.getGroupId(), eventType, result.getMessage());
            } else {
                group.getArchiveEvents().add(new ArchiveEvent(eventType, adapter.getName(), EventStatus.SUCCESS, null, Instant.now()));
                log.info("Adapter {} notified for group {} on {}", adapter.getName(), group.getGroupId(), eventType);
            }
        }

        if (!newErrors.isEmpty()) {
            group.getErrors().addAll(newErrors);
        }

        if ("GROUP_CLOSED".equals(eventType)) {
            if (newErrors.isEmpty()) {
                group.setStatus(ArchiveStatus.ARCHIVED);
            } else {
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
        }

        group.setUpdatedAt(Instant.now());
        repository.save(group);
    }

    public void retryGroup(ArchiveGroup group) {
        log.info("Manual retry triggered for group {}", group.getGroupId());
        notifyAdapters(group, "GROUP_CLOSED");
    }

    public void retryFailed() {
        List<ArchiveGroup> pending = repository.findByStatus(ArchiveStatus.PENDING);
        List<ArchiveGroup> retryable = pending.stream()
                .filter(g -> g.getRetryCount() > 0 && g.getRetryCount() < MAX_RETRIES)
                .collect(Collectors.toList());

        for (ArchiveGroup group : retryable) {
            log.info("Retrying archive for group {} (attempt {}/{})",
                    group.getGroupId(), group.getRetryCount() + 1, MAX_RETRIES);
            notifyAdapters(group, "GROUP_CLOSED");
        }
    }

    private ArchiveRequest toArchiveRequest(ArchiveGroup group, String eventType) {
        List<ArchiveRequest.LogEntry> entries = group.getEntries().stream()
                .map(e -> new ArchiveRequest.LogEntry(e.getEntryId(), e.getContent(), e.getTimestamp()))
                .collect(Collectors.toList());
        return new ArchiveRequest(eventType, group.getGroupId(), group.getName(), entries);
    }
}
