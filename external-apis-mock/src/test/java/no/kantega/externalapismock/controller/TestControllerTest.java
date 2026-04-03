package no.kantega.externalapismock.controller;

import no.kantega.externalapismock.service.MockService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TestController.class)
class TestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MockService mockService;

    @TestConfiguration
    static class Config {
        @Bean
        public MockService mockService() {
            return new MockService();
        }
    }

    @Test
    void setup_configuresMock() throws Exception {
        mockMvc.perform(post("/api/test/setup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"endpoint\":\"noarka\",\"statusCode\":500,\"body\":\"{\\\"err\\\":true}\",\"delayMs\":0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("configured"))
                .andExpect(jsonPath("$.endpoint").value("noarka"));
    }

    @Test
    void reset_clearsAll() throws Exception {
        mockService.recordRequest("noarka", "POST", "/test", "body");

        mockMvc.perform(post("/api/test/reset"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("reset"));

        mockMvc.perform(get("/api/test/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void config_returnsDefaultConfigs() throws Exception {
        mockService.reset();

        mockMvc.perform(get("/api/test/config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.noarka.statusCode").value(200))
                .andExpect(jsonPath("$.noarka.delayMs").value(0))
                .andExpect(jsonPath("$.noarkb.statusCode").value(200))
                .andExpect(jsonPath("$.noarkb.delayMs").value(0));
    }

    @Test
    void config_returnsCustomConfigAfterSetup() throws Exception {
        mockMvc.perform(post("/api/test/setup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"endpoint\":\"noarka\",\"statusCode\":500,\"delayMs\":100}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/test/config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.noarka.statusCode").value(500))
                .andExpect(jsonPath("$.noarka.delayMs").value(100))
                .andExpect(jsonPath("$.noarkb.statusCode").value(200));
    }

    @Test
    void history_returnsRecordedRequests() throws Exception {
        mockService.reset();
        mockService.recordRequest("noarka", "POST", "/api/noarka/archive", "{\"data\":1}");

        mockMvc.perform(get("/api/test/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].endpoint").value("noarka"))
                .andExpect(jsonPath("$[0].method").value("POST"));
    }
}
