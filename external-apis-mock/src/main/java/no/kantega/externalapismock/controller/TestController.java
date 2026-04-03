package no.kantega.externalapismock.controller;

import no.kantega.externalapismock.model.MockSetupRequest;
import no.kantega.externalapismock.model.ReceivedRequest;
import no.kantega.externalapismock.service.MockService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import no.kantega.externalapismock.model.MockConfig;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/test")
public class TestController {

    private final MockService mockService;

    public TestController(MockService mockService) {
        this.mockService = mockService;
    }

    @PostMapping("/setup")
    public ResponseEntity<Map<String, String>> setup(@RequestBody MockSetupRequest request) {
        mockService.setup(request);
        return ResponseEntity.ok(Map.of("status", "configured", "endpoint", request.getEndpoint()));
    }

    @PostMapping("/reset")
    public ResponseEntity<Map<String, String>> reset() {
        mockService.reset();
        return ResponseEntity.ok(Map.of("status", "reset"));
    }

    @GetMapping("/config")
    public ResponseEntity<Map<String, MockConfig>> config() {
        return ResponseEntity.ok(mockService.getAllConfigs());
    }

    @GetMapping("/history")
    public ResponseEntity<List<ReceivedRequest>> history() {
        return ResponseEntity.ok(mockService.getHistory());
    }
}
