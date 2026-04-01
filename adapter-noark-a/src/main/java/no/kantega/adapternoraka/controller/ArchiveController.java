package no.kantega.adapternoraka.controller;

import no.kantega.adapternoraka.model.ArchiveRequest;
import no.kantega.adapternoraka.model.ArchiveResult;
import no.kantega.adapternoraka.service.ArchiveService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/archive")
public class ArchiveController {

    private final ArchiveService archiveService;

    public ArchiveController(ArchiveService archiveService) {
        this.archiveService = archiveService;
    }

    @PostMapping
    public ResponseEntity<ArchiveResult> archive(@RequestBody ArchiveRequest request) {
        ArchiveResult result = archiveService.archive(request);
        if (result.isSuccess()) {
            return ResponseEntity.ok(result);
        }
        return ResponseEntity.status(502).body(result);
    }
}
