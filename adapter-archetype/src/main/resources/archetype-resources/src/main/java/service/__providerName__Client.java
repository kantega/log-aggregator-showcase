#set( $symbol_dollar = '$' )
package ${package}.service;

import ${package}.model.${providerName}Payload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class ${providerName}Client {

    private static final Logger log = LoggerFactory.getLogger(${providerName}Client.class);

    private final RestClient restClient;
    private final String ${providerKey}Url;

    @Autowired
    public ${providerName}Client(@Value("${symbol_dollar}{${providerSlug}.api.url}") String ${providerKey}Url) {
        this.${providerKey}Url = ${providerKey}Url;
        this.restClient = RestClient.builder()
                .baseUrl(${providerKey}Url)
                .build();
    }

    // Visible for testing
    ${providerName}Client(RestClient restClient, String ${providerKey}Url) {
        this.restClient = restClient;
        this.${providerKey}Url = ${providerKey}Url;
    }

    /**
     * POST the transformed payload to the ${providerName} provider.
     *
     * TODO(skill): Set the correct endpoint path (`uri(...)`) based on the
     * ${providerName} OpenAPI spec — e.g. `/records`, `/archive`, `/documents`.
     * The default below is `/archive` which works against the default mock but
     * is probably not what the real ${providerName} expects.
     */
    public String postArchive(${providerName}Payload payload) {
        log.info("Posting archive to ${providerName} at {}", ${providerKey}Url);

        return restClient.post()
                .uri("/archive")
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .body(String.class);
    }
}
