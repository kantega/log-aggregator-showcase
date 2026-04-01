package no.kantega.adapternoraka.service;

import no.kantega.adapternoraka.model.NoarkAPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class NoarkAClient {

    private static final Logger log = LoggerFactory.getLogger(NoarkAClient.class);

    private final RestClient restClient;
    private final String noarkAUrl;

    @Autowired
    public NoarkAClient(@Value("${noark-a.api.url}") String noarkAUrl) {
        this.noarkAUrl = noarkAUrl;
        this.restClient = RestClient.builder()
                .baseUrl(noarkAUrl)
                .build();
    }

    // Visible for testing
    NoarkAClient(RestClient restClient, String noarkAUrl) {
        this.restClient = restClient;
        this.noarkAUrl = noarkAUrl;
    }

    public String postArchive(NoarkAPayload payload) {
        log.info("Posting JSON archive to Noark A at {}", noarkAUrl);

        return restClient.post()
                .uri("/archive")
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .body(String.class);
    }
}
