package no.kantega.externalapismock.service;

import no.kantega.externalapismock.model.MockConfig;
import no.kantega.externalapismock.model.MockSetupRequest;
import no.kantega.externalapismock.model.ReceivedRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MockServiceTest {

    private MockService mockService;

    @BeforeEach
    void setUp() {
        mockService = new MockService();
    }

    @Test
    void getConfig_returnsDefaultWhenNotConfigured() {
        MockConfig config = mockService.getConfig("noarka");
        assertThat(config.getStatusCode()).isEqualTo(200);
        assertThat(config.getDelayMs()).isZero();
    }

    @Test
    void setup_storesConfiguration() {
        MockSetupRequest request = new MockSetupRequest();
        request.setEndpoint("noarka");
        request.setStatusCode(500);
        request.setBody("{\"error\": \"fail\"}");
        request.setDelayMs(100);

        mockService.setup(request);

        MockConfig config = mockService.getConfig("noarka");
        assertThat(config.getStatusCode()).isEqualTo(500);
        assertThat(config.getBody()).isEqualTo("{\"error\": \"fail\"}");
        assertThat(config.getDelayMs()).isEqualTo(100);
    }

    @Test
    void setup_doesNotAffectOtherEndpoints() {
        MockSetupRequest request = new MockSetupRequest();
        request.setEndpoint("noarka");
        request.setStatusCode(500);
        mockService.setup(request);

        MockConfig noarkbConfig = mockService.getConfig("noarkb");
        assertThat(noarkbConfig.getStatusCode()).isEqualTo(200);
    }

    @Test
    void recordRequest_addsToHistory() {
        mockService.recordRequest("noarka", "POST", "/api/noarka/archive", "{\"data\":1}");
        mockService.recordRequest("noarkb", "POST", "/api/noarkb/archive", "{\"data\":2}");

        List<ReceivedRequest> history = mockService.getHistory();
        assertThat(history).hasSize(2);
        assertThat(history.get(0).getEndpoint()).isEqualTo("noarka");
        assertThat(history.get(1).getEndpoint()).isEqualTo("noarkb");
    }

    @Test
    void reset_clearsConfigAndHistory() {
        MockSetupRequest request = new MockSetupRequest();
        request.setEndpoint("noarka");
        request.setStatusCode(500);
        mockService.setup(request);
        mockService.recordRequest("noarka", "POST", "/test", "body");

        mockService.reset();

        assertThat(mockService.getConfig("noarka").getStatusCode()).isEqualTo(200);
        assertThat(mockService.getHistory()).isEmpty();
    }

    @Test
    void getHistory_returnsUnmodifiableList() {
        mockService.recordRequest("noarka", "GET", "/test", null);
        List<ReceivedRequest> history = mockService.getHistory();

        assertThat(history).hasSize(1);
        org.junit.jupiter.api.Assertions.assertThrows(UnsupportedOperationException.class,
                () -> history.add(new ReceivedRequest()));
    }
}
