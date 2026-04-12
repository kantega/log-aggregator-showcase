#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.service;

import ${package}.model.MockConfig;
import ${package}.model.MockSetupRequest;
import ${package}.model.ReceivedRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MockServiceTest {

    private MockService mockService;

    @BeforeEach
    void setUp() {
        mockService = new MockService();
    }

    @Test
    void getConfig_returnsDefaultWhenNotConfigured() {
        MockConfig config = mockService.getConfig("provider1");
        assertThat(config.getStatusCode()).isEqualTo(200);
        assertThat(config.getDelayMs()).isZero();
    }

    @Test
    void setup_storesConfiguration() {
        MockSetupRequest request = new MockSetupRequest();
        request.setEndpoint("provider1");
        request.setStatusCode(500);
        request.setBody("{\"error\": \"fail\"}");
        request.setDelayMs(100);

        mockService.setup(request);

        MockConfig config = mockService.getConfig("provider1");
        assertThat(config.getStatusCode()).isEqualTo(500);
        assertThat(config.getBody()).isEqualTo("{\"error\": \"fail\"}");
        assertThat(config.getDelayMs()).isEqualTo(100);
    }

    @Test
    void setup_doesNotAffectOtherEndpoints() {
        MockSetupRequest request = new MockSetupRequest();
        request.setEndpoint("provider1");
        request.setStatusCode(500);
        mockService.setup(request);

        MockConfig provider2Config = mockService.getConfig("provider2");
        assertThat(provider2Config.getStatusCode()).isEqualTo(200);
    }

    @Test
    void recordRequest_addsToHistory() {
        mockService.recordRequest("provider1", "POST", "/api/provider1/archive", "{\"data\":1}");
        mockService.recordRequest("provider2", "POST", "/api/provider2/archive", "{\"data\":2}");

        List<ReceivedRequest> history = mockService.getHistory();
        assertThat(history).hasSize(2);
        assertThat(history.get(0).getEndpoint()).isEqualTo("provider1");
        assertThat(history.get(1).getEndpoint()).isEqualTo("provider2");
    }

    @Test
    void reset_clearsConfigAndHistory() {
        MockSetupRequest request = new MockSetupRequest();
        request.setEndpoint("provider1");
        request.setStatusCode(500);
        mockService.setup(request);
        mockService.recordRequest("provider1", "POST", "/test", "body");

        mockService.reset();

        assertThat(mockService.getConfig("provider1").getStatusCode()).isEqualTo(200);
        assertThat(mockService.getHistory()).isEmpty();
    }

    @Test
    void getHistory_returnsUnmodifiableList() {
        mockService.recordRequest("provider1", "GET", "/test", null);
        List<ReceivedRequest> history = mockService.getHistory();

        assertThat(history).hasSize(1);
        org.junit.jupiter.api.Assertions.assertThrows(UnsupportedOperationException.class,
                () -> history.add(new ReceivedRequest()));
    }

    @Test
    void getAllConfigs_returnsDynamicMap() {
        assertThat(mockService.getAllConfigs()).isEmpty();

        MockSetupRequest request = new MockSetupRequest();
        request.setEndpoint("provider1");
        request.setStatusCode(503);
        mockService.setup(request);

        Map<String, MockConfig> configs = mockService.getAllConfigs();
        assertThat(configs).hasSize(1);
        assertThat(configs).containsKey("provider1");
        assertThat(configs.get("provider1").getStatusCode()).isEqualTo(503);
    }
}
