package no.kantega.externalapismock.service;

import no.kantega.externalapismock.model.MockConfig;
import no.kantega.externalapismock.model.MockSetupRequest;
import no.kantega.externalapismock.model.ReceivedRequest;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class MockService {

    private final Map<String, MockConfig> configs = new ConcurrentHashMap<>();
    private final List<ReceivedRequest> history = new CopyOnWriteArrayList<>();

    public MockConfig getConfig(String endpoint) {
        return configs.getOrDefault(endpoint, new MockConfig());
    }

    public void setup(MockSetupRequest request) {
        MockConfig config = new MockConfig();
        config.setStatusCode(request.getStatusCode());
        if (request.getBody() != null) {
            config.setBody(request.getBody());
        }
        config.setDelayMs(request.getDelayMs());
        if (request.getFailResponses() != null && !request.getFailResponses().isEmpty()) {
            config.getFailResponses().addAll(request.getFailResponses());
        }
        configs.put(request.getEndpoint(), config);
    }

    /**
     * If the endpoint has queued failure responses, removes and returns the next one.
     * Returns {@code null} when the queue is empty — callers should fall back to the normal config.
     */
    public Integer consumeFailResponse(String endpoint) {
        MockConfig config = configs.get(endpoint);
        if (config == null) {
            return null;
        }
        List<Integer> queue = config.getFailResponses();
        if (queue.isEmpty()) {
            return null;
        }
        synchronized (queue) {
            if (queue.isEmpty()) {
                return null;
            }
            return queue.remove(0);
        }
    }

    public void reset() {
        configs.clear();
        history.clear();
    }

    public void recordRequest(String endpoint, String method, String path, String body) {
        history.add(new ReceivedRequest(endpoint, method, path, body, Instant.now()));
    }

    public List<ReceivedRequest> getHistory() {
        return Collections.unmodifiableList(new ArrayList<>(history));
    }

    public Map<String, MockConfig> getAllConfigs() {
        Map<String, MockConfig> result = new java.util.HashMap<>();
        result.put("noarka", getConfig("noarka"));
        result.put("noarkb", getConfig("noarkb"));
        return result;
    }
}
