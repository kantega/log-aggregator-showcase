package no.kantega.externalapismock.model;

import lombok.Data;

@Data
public class MockConfig {
    private int statusCode = 200;
    private String body = "{\"status\": \"ok\"}";
    private long delayMs = 0;
}
