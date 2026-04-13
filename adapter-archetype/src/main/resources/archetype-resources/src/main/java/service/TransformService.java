package ${package}.service;

import ${package}.model.${providerName}Payload;
import ${package}.model.ArchiveRequest;
import org.springframework.stereotype.Service;

@Service
public class TransformService {

    /**
     * Convert an Edge {@link ArchiveRequest} into a {@link ${providerName}Payload}.
     *
     * TODO(skill): Map fields from the ArchiveRequest into the provider-specific
     * payload structure defined by the ${providerName} OpenAPI spec. Populate
     * collections (documents, attachments, etc.) from request.getEntries().
     *
     * For now this returns an empty payload so the scaffold compiles.
     */
    public ${providerName}Payload transform(ArchiveRequest request) {
        return new ${providerName}Payload();
    }
}
