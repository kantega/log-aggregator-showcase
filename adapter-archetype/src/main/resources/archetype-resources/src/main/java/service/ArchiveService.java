package ${package}.service;

import ${package}.model.${providerName}Payload;
import ${package}.model.ArchiveRequest;
import ${package}.model.ArchiveResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ArchiveService {

    private static final Logger log = LoggerFactory.getLogger(ArchiveService.class);

    private final TransformService transformService;
    private final ${providerName}Client ${providerKey}Client;

    public ArchiveService(TransformService transformService, ${providerName}Client ${providerKey}Client) {
        this.transformService = transformService;
        this.${providerKey}Client = ${providerKey}Client;
    }

    public ArchiveResult archive(ArchiveRequest request) {
        try {
            log.info("Archiving group {} ({}) to ${providerName}", request.getGroupId(), request.getGroupName());

            ${providerName}Payload payload = transformService.transform(request);
            ${providerKey}Client.postArchive(payload);

            log.info("Successfully archived group {} to ${providerName}", request.getGroupId());
            return ArchiveResult.ok();
        } catch (Exception e) {
            log.error("Failed to archive group {} to ${providerName}: {}", request.getGroupId(), e.getMessage());
            return ArchiveResult.fail(e.getMessage());
        }
    }
}
