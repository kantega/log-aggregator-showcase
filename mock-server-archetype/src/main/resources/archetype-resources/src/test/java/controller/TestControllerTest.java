#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.controller;

import ${package}.service.MockService;
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
                        .content("{\"endpoint\":\"provider1\",\"statusCode\":500,\"delayMs\":0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("${symbol_dollar}.status").value("configured"))
                .andExpect(jsonPath("${symbol_dollar}.endpoint").value("provider1"));
    }

    @Test
    void reset_clearsAll() throws Exception {
        mockService.recordRequest("provider1", "POST", "/test", "body");

        mockMvc.perform(post("/api/test/reset"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("${symbol_dollar}.status").value("reset"));

        mockMvc.perform(get("/api/test/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("${symbol_dollar}.length()").value(0));
    }

    @Test
    void config_returnsEmptyWhenNothingConfigured() throws Exception {
        mockService.reset();

        mockMvc.perform(get("/api/test/config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("${symbol_dollar}.length()").value(0));
    }

    @Test
    void config_returnsConfiguredEndpointAfterSetup() throws Exception {
        mockService.reset();

        mockMvc.perform(post("/api/test/setup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"endpoint\":\"provider1\",\"statusCode\":500,\"delayMs\":100}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/test/config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("${symbol_dollar}.provider1.statusCode").value(500))
                .andExpect(jsonPath("${symbol_dollar}.provider1.delayMs").value(100));
    }

    @Test
    void history_returnsRecordedRequests() throws Exception {
        mockService.reset();
        mockService.recordRequest("provider1", "POST", "/api/provider1/archive", "{\"data\":1}");

        mockMvc.perform(get("/api/test/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("${symbol_dollar}.length()").value(1))
                .andExpect(jsonPath("${symbol_dollar}[0].endpoint").value("provider1"))
                .andExpect(jsonPath("${symbol_dollar}[0].method").value("POST"));
    }
}
