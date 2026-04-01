package no.kantega.edge.service;

import no.kantega.edge.model.ArchiveRequest;
import no.kantega.edge.model.ArchiveResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class AdapterClient {

    private static final Logger log = LoggerFactory.getLogger(AdapterClient.class);

    private final RestClient restClient;

    public AdapterClient() {
        this.restClient = RestClient.create();
    }

    // Visible for testing
    AdapterClient(RestClient restClient) {
        this.restClient = restClient;
    }

    public ArchiveResult sendToAdapter(String adapterUrl, ArchiveRequest request) {
        try {
            log.info("Sending archive request for group {} to {}", request.getGroupId(), adapterUrl);

            ArchiveResult result = restClient.post()
                    .uri(adapterUrl + "/archive")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(ArchiveResult.class);

            return result != null ? result : new ArchiveResult(false, "Null response from adapter", "unknown");
        } catch (Exception e) {
            log.error("Error calling adapter at {}: {}", adapterUrl, e.getMessage());
            return new ArchiveResult(false, e.getMessage(), "unknown");
        }
    }
}
