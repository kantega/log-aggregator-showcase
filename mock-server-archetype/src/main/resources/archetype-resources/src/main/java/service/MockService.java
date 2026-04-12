#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.service;

import ${package}.model.MockConfig;
import ${package}.model.MockSetupRequest;
import ${package}.model.ReceivedRequest;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
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
        configs.put(request.getEndpoint(), config);
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
        return Collections.unmodifiableMap(new HashMap<>(configs));
    }
}
