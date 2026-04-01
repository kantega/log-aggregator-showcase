package no.kantega.adapternorakb.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class NoarkBClient {

    private static final Logger log = LoggerFactory.getLogger(NoarkBClient.class);

    private final RestClient restClient;
    private final String noarkBUrl;

    @Autowired
    public NoarkBClient(@Value("${noark-b.api.url}") String noarkBUrl) {
        this.noarkBUrl = noarkBUrl;
        this.restClient = RestClient.builder()
                .baseUrl(noarkBUrl)
                .build();
    }

    // Visible for testing
    NoarkBClient(RestClient restClient, String noarkBUrl) {
        this.restClient = restClient;
        this.noarkBUrl = noarkBUrl;
    }

    public String postArchive(byte[] zipData) {
        log.info("Posting ZIP archive to Noark B at {}", noarkBUrl);

        return restClient.post()
                .uri("/archive")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(zipData)
                .retrieve()
                .body(String.class);
    }
}
