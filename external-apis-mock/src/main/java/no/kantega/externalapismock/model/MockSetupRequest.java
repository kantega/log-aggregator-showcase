package no.kantega.externalapismock.model;

import lombok.Data;

@Data
public class MockSetupRequest {
    private String endpoint; // "noarka" or "noarkb"
    private int statusCode = 200;
    private String body;
    private long delayMs = 0;
}
