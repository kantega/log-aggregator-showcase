---
name: add-mock-provider
description: Add a new provider RestController to the external-apis-mock server. Given a provider's OpenAPI spec, generates the controller, test, and wires it into the existing mock infrastructure.
---

# Add Mock Provider

Add a new external API provider to the `external-apis-mock` service. This creates a RestController that simulates the provider's API with configurable responses, delays, and request recording.

## Prerequisites

The mock server must already exist with this structure (generated from `mock-server-archetype` or matching the pattern in `external-apis-mock/`):

- `service/MockService.java` — thread-safe config/history store
- `controller/TestController.java` — `/api/test/setup`, `/reset`, `/history`, `/config`
- `model/MockConfig.java`, `MockSetupRequest.java`, `ReceivedRequest.java`

## Input

The user provides the provider's real **OpenAPI spec** (YAML or JSON). Read it to understand:

- The provider's name (from `info.title` or user input)
- What HTTP methods the provider accepts (POST, GET, PUT, etc.)
- The general shape of requests — this helps you understand what the mock will receive, but the mock itself uses a wildcard catch-all pattern, not individual routes

## Steps

### 1. Derive naming from the provider

Ask the user or derive from the OpenAPI spec:

| Value | Convention | Example |
|-------|-----------|---------|
| Endpoint name | lowercase, no hyphens | `noarkc` |
| Controller class | PascalCase + `Controller` | `NoarkCController` |
| Test class | PascalCase + `ControllerTest` | `NoarkCControllerTest` |
| Request mapping | `/api/{endpoint}` | `/api/noarkc` |

### 2. Generate the controller

Create `src/main/java/{package}/controller/{Name}Controller.java` following this exact pattern:

```java
package {package}.controller;

import jakarta.servlet.http.HttpServletRequest;
import {package}.model.MockConfig;
import {package}.service.MockService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/{endpoint}")
public class {Name}Controller {

    private final MockService mockService;

    public {Name}Controller(MockService mockService) {
        this.mockService = mockService;
    }

    @PostMapping("/**")
    public ResponseEntity<String> handlePost(@RequestBody(required = false) String body,
                                             HttpServletRequest request) {
        return handle("POST", body, request);
    }

    @GetMapping("/**")
    public ResponseEntity<String> handleGet(HttpServletRequest request) {
        return handle("GET", null, request);
    }

    @PutMapping("/**")
    public ResponseEntity<String> handlePut(@RequestBody(required = false) String body,
                                            HttpServletRequest request) {
        return handle("PUT", body, request);
    }

    private ResponseEntity<String> handle(String method, String body, HttpServletRequest request) {
        MockConfig config = mockService.getConfig("{endpoint}");
        mockService.recordRequest("{endpoint}", method, request.getRequestURI(), body);

        if (config.getDelayMs() > 0) {
            try {
                Thread.sleep(config.getDelayMs());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        return ResponseEntity.status(config.getStatusCode()).body(config.getBody());
    }
}
```

Key points:
- The `{endpoint}` string (e.g., `"noarkc"`) must be consistent everywhere: `@RequestMapping`, `getConfig()`, and `recordRequest()`
- `@RequestBody(required = false)` — the mock accepts any body, including none
- Wildcard `/**` mapping catches all sub-paths
- Delay uses `Thread.sleep` with proper interrupt handling
- Constructor injection, not field injection

### 3. Generate the controller test

Create `src/test/java/{package}/controller/{Name}ControllerTest.java`:

```java
package {package}.controller;

import {package}.model.MockSetupRequest;
import {package}.service.MockService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({Name}Controller.class)
class {Name}ControllerTest {

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
        mockMvc.perform(post("/api/{endpoint}/archive")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"data\":\"test\"}"))
                .andExpect(status().isOk())
                .andExpect(content().string("{\"status\": \"ok\"}"));
    }

    @Test
    void post_returnsConfiguredStatus() throws Exception {
        MockSetupRequest setup = new MockSetupRequest();
        setup.setEndpoint("{endpoint}");
        setup.setStatusCode(503);
        setup.setBody("{\"error\":\"unavailable\"}");
        mockService.setup(setup);

        mockMvc.perform(post("/api/{endpoint}/archive")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"data\":\"test\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().string("{\"error\":\"unavailable\"}"));
    }

    @Test
    void get_returnsOk() throws Exception {
        mockMvc.perform(get("/api/{endpoint}/status"))
                .andExpect(status().isOk());
    }

    @Test
    void post_recordsRequest() throws Exception {
        mockService.reset();

        mockMvc.perform(post("/api/{endpoint}/test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"payload\":true}"))
                .andExpect(status().isOk());

        var history = mockService.getHistory();
        org.assertj.core.api.Assertions.assertThat(history).hasSize(1);
        org.assertj.core.api.Assertions.assertThat(history.get(0).getEndpoint()).isEqualTo("{endpoint}");
        org.assertj.core.api.Assertions.assertThat(history.get(0).getMethod()).isEqualTo("POST");
    }
}
```

### 4. Update OpenAPI baseline

Delete `src/test/resources/openapi-baseline.json` so it regenerates on the next test run with the new endpoint included.

### 5. Check for hardcoded endpoint lists

If `MockService.getAllConfigs()` hardcodes endpoint names (older pattern), refactor it to return all keys dynamically:

```java
public Map<String, MockConfig> getAllConfigs() {
    return Collections.unmodifiableMap(new HashMap<>(configs));
}
```

Also update any tests that assert on specific hardcoded endpoint names in the config response.

### 6. Verify

```bash
cd external-apis-mock && mvn test
```

All existing tests must still pass. The new controller test must pass. The OpenAPI baseline will be recreated automatically.

## Checklist

- [ ] Controller created with correct endpoint name in `@RequestMapping`, `getConfig()`, and `recordRequest()`
- [ ] Controller test created with `@WebMvcTest` and `@TestConfiguration` bean
- [ ] `openapi-baseline.json` deleted
- [ ] `getAllConfigs()` returns dynamic map (no hardcoded endpoint names)
- [ ] `mvn test` passes
