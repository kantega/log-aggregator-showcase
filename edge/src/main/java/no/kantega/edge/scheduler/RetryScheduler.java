package no.kantega.edge.scheduler;

import no.kantega.edge.service.ArchiveService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class RetryScheduler {

    private static final Logger log = LoggerFactory.getLogger(RetryScheduler.class);

    private final ArchiveService archiveService;

    public RetryScheduler(ArchiveService archiveService) {
        this.archiveService = archiveService;
    }

    @Scheduled(fixedDelay = 1000) // every 1 second; retryDue() gates on per-group backoff
    public void retryPending() {
        log.debug("Running scheduled retry check");
        archiveService.retryDue();
    }
}
