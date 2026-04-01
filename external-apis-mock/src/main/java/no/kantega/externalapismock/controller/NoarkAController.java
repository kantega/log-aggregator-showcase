package no.kantega.externalapismock.controller;

import jakarta.servlet.http.HttpServletRequest;
import no.kantega.externalapismock.model.MockConfig;
import no.kantega.externalapismock.service.MockService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/noarka")
public class NoarkAController {

    private final MockService mockService;

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
