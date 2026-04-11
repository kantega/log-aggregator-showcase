package no.kantega.externalapismock.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import no.kantega.externalapismock.model.MockConfig;
import no.kantega.externalapismock.service.MockService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/noarka")
public class NoarkAController {

    private final MockService mockService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public NoarkAController(MockService mockService) {
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
        MockConfig config = mockService.getConfig("noarka");
        mockService.recordRequest("noarka", method, request.getRequestURI(), body);

        if ("POST".equals(method) && body != null) {
            ResponseEntity<String> validationError = validateContent(body);
            if (validationError != null) {
                return validationError;
            }
        }

        if (config.getDelayMs() > 0) {
            try {
                Thread.sleep(config.getDelayMs());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        return ResponseEntity.status(config.getStatusCode()).body(config.getBody());
    }

    private ResponseEntity<String> validateContent(String body) {
        try {
            JsonNode root = objectMapper.readTree(body);
            String eventType = root.has("eventType") ? root.get("eventType").asText() : null;
            if (!"ENTRY_ADDED".equals(eventType)) {
                return null;
            }
            JsonNode documents = root.get("documents");
            if (documents != null && documents.isArray()) {
                for (JsonNode doc : documents) {
                    JsonNode content = doc.get("content");
                    if (content != null && content.asText().toLowerCase().contains("error")) {
                        return ResponseEntity.badRequest()
                                .body("{\"error\": \"Content validation failed: entry contains forbidden text\"}");
                    }
                }
            }
        } catch (Exception ignored) {
            // Not valid JSON or missing fields — skip validation
        }
        return null;
    }
}
