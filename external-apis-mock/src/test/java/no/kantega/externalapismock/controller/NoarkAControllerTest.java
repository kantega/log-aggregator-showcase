package no.kantega.externalapismock.controller;

import no.kantega.externalapismock.model.MockSetupRequest;
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

@WebMvcTest(NoarkAController.class)
class NoarkAControllerTest {

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
    void post_returnsDefaultOk() throws Exception {
        mockMvc.perform(post("/api/noarka/archive")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"data\":\"test\"}"))
                .andExpect(status().isOk())
                .andExpect(content().string("{\"status\": \"ok\"}"));
    }

    @Test
    void post_returnsConfiguredStatus() throws Exception {
        MockSetupRequest setup = new MockSetupRequest();
        setup.setEndpoint("noarka");
        setup.setStatusCode(503);
        setup.setBody("{\"error\":\"unavailable\"}");
        mockService.setup(setup);

        mockMvc.perform(post("/api/noarka/archive")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"data\":\"test\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().string("{\"error\":\"unavailable\"}"));
    }

    @Test
    void get_returnsOk() throws Exception {
        mockMvc.perform(get("/api/noarka/status"))
                .andExpect(status().isOk());
    }

    @Test
    void post_recordsRequest() throws Exception {
        mockService.reset();

        mockMvc.perform(post("/api/noarka/test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"payload\":true}"))
                .andExpect(status().isOk());

        var history = mockService.getHistory();
        org.assertj.core.api.Assertions.assertThat(history).hasSize(1);
        org.assertj.core.api.Assertions.assertThat(history.get(0).getEndpoint()).isEqualTo("noarka");
        org.assertj.core.api.Assertions.assertThat(history.get(0).getMethod()).isEqualTo("POST");
    }
}
