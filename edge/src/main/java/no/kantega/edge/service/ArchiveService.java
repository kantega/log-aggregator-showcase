package no.kantega.edge.service;

import no.kantega.edge.model.ArchiveGroup;
import no.kantega.edge.model.ArchiveGroup.ArchiveError;
import no.kantega.edge.model.ArchiveGroup.ArchiveStatus;
import no.kantega.edge.model.ArchiveRequest;
import no.kantega.edge.model.ArchiveResult;
import no.kantega.edge.repository.ArchiveGroupRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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
    private final String adapterNoarkAUrl;
    private final String adapterNoarkBUrl;

    public ArchiveService(
            ArchiveGroupRepository repository,
            AdapterClient adapterClient,
            @Value("${adapter.noark-a.url}") String adapterNoarkAUrl,
            @Value("${adapter.noark-b.url}") String adapterNoarkBUrl) {
        this.repository = repository;
        this.adapterClient = adapterClient;
        this.adapterNoarkAUrl = adapterNoarkAUrl;
        this.adapterNoarkBUrl = adapterNoarkBUrl;
    }

    public void archiveGroup(ArchiveGroup group) {
        group.setStatus(ArchiveStatus.IN_PROGRESS);
        group.setUpdatedAt(Instant.now());
        repository.save(group);

        ArchiveRequest request = toArchiveRequest(group);

        ArchiveResult resultA = adapterClient.sendToAdapter(adapterNoarkAUrl, request);
        ArchiveResult resultB = adapterClient.sendToAdapter(adapterNoarkBUrl, request);

        List<ArchiveError> newErrors = new ArrayList<>();
        if (!resultA.isSuccess()) {
            newErrors.add(new ArchiveError("noark-a", resultA.getMessage(), Instant.now()));
        }
        if (!resultB.isSuccess()) {
            newErrors.add(new ArchiveError("noark-b", resultB.getMessage(), Instant.now()));
        }

        if (newErrors.isEmpty()) {
            group.setStatus(ArchiveStatus.ARCHIVED);
            log.info("Group {} archived successfully to both adapters", group.getGroupId());
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
