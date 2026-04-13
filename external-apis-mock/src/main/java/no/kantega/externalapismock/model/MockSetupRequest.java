package no.kantega.externalapismock.model;

import lombok.Data;

import java.util.List;

@Data
public class MockSetupRequest {
    private String endpoint; // "noarka" or "noarkb"
    private int statusCode = 200;
    private String body;
    private long delayMs = 0;
    /**
     * Optional queue of status codes to return on the next N requests.
     * Each request consumes one entry; when the queue is empty, requests fall back to 200.
     * Overrides {@link #statusCode}/{@link #body} while the queue has entries.
     */
    private List<Integer> failResponses;
}
