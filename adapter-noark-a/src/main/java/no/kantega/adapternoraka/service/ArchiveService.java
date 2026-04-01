package no.kantega.adapternoraka.service;

import no.kantega.adapternoraka.model.ArchiveRequest;
import no.kantega.adapternoraka.model.ArchiveResult;
import no.kantega.adapternoraka.model.NoarkAPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ArchiveService {

    private static final Logger log = LoggerFactory.getLogger(ArchiveService.class);

    private final TransformService transformService;
    private final NoarkAClient noarkAClient;

    public ArchiveService(TransformService transformService, NoarkAClient noarkAClient) {
        this.transformService = transformService;
        this.noarkAClient = noarkAClient;
    }

    public ArchiveResult archive(ArchiveRequest request) {
        try {
            log.info("Archiving group {} ({}) to Noark A", request.getGroupId(), request.getGroupName());

            NoarkAPayload payload = transformService.transform(request);
            String response = noarkAClient.postArchive(payload);

            log.info("Successfully archived group {} to Noark A", request.getGroupId());
            return ArchiveResult.ok();
        } catch (Exception e) {
            log.error("Failed to archive group {} to Noark A: {}", request.getGroupId(), e.getMessage());
            return ArchiveResult.fail(e.getMessage());
        }
    }
}
