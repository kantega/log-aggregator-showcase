package no.kantega.externalapismock.model;

import lombok.Data;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Data
public class MockConfig {
    private int statusCode = 200;
    private String body = "{\"status\": \"ok\"}";
    private long delayMs = 0;
    /** Remaining queued failure status codes; consumed one per incoming request. */
    private List<Integer> failResponses = new CopyOnWriteArrayList<>();
}
