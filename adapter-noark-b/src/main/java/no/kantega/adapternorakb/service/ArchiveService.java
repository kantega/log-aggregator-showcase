package no.kantega.adapternorakb.service;

import no.kantega.adapternorakb.model.ArchiveRequest;
import no.kantega.adapternorakb.model.ArchiveResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ArchiveService {

    private static final Logger log = LoggerFactory.getLogger(ArchiveService.class);

    private final ZipService zipService;
    private final NoarkBClient noarkBClient;

    public ArchiveService(ZipService zipService, NoarkBClient noarkBClient) {
        this.zipService = zipService;
        this.noarkBClient = noarkBClient;
    }

    public ArchiveResult archive(ArchiveRequest request) {
        if ("ENTRY_ADDED".equals(request.getEventType())) {
            log.info("Skipping ENTRY_ADDED for group {} — Noark B only archives on GROUP_CLOSED", request.getGroupId());
            return ArchiveResult.ok();
        }

        try {
            log.info("Archiving group {} ({}) to Noark B", request.getGroupId(), request.getGroupName());

            byte[] zipData = zipService.createZip(request);
            String response = noarkBClient.postArchive(zipData);

            log.info("Successfully archived group {} to Noark B ({} bytes)", request.getGroupId(), zipData.length);
            return ArchiveResult.ok();
        } catch (Exception e) {
            log.error("Failed to archive group {} to Noark B: {}", request.getGroupId(), e.getMessage());
            return ArchiveResult.fail(e.getMessage());
        }
    }
}
