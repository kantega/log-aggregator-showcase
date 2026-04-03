package no.kantega.edge.controller;

import no.kantega.edge.model.ArchiveGroup;
import no.kantega.edge.repository.ArchiveGroupRepository;
import no.kantega.edge.service.ArchiveService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api")
public class StatusController {

    private final ArchiveGroupRepository repository;
    private final ArchiveService archiveService;

    public StatusController(ArchiveGroupRepository repository, ArchiveService archiveService) {
        this.repository = repository;
        this.archiveService = archiveService;
    }

    @GetMapping({"/status", "/archive-state"})
    public ResponseEntity<Map<String, Object>> getOverview() {
        List<ArchiveGroup> all = repository.findAll();
        Map<String, Object> overview = new HashMap<>();
        overview.put("total", all.size());
        overview.put("pending", all.stream().filter(g -> g.getStatus() == ArchiveGroup.ArchiveStatus.PENDING).count());
        overview.put("inProgress", all.stream().filter(g -> g.getStatus() == ArchiveGroup.ArchiveStatus.IN_PROGRESS).count());
        overview.put("archived", all.stream().filter(g -> g.getStatus() == ArchiveGroup.ArchiveStatus.ARCHIVED).count());
        overview.put("failed", all.stream().filter(g -> g.getStatus() == ArchiveGroup.ArchiveStatus.FAILED).count());
        return ResponseEntity.ok(overview);
    }

    @GetMapping("/groups")
    public ResponseEntity<List<ArchiveGroup>> getAllGroups() {
        return ResponseEntity.ok(repository.findAll());
    }

    @GetMapping("/groups/{groupId}")
    public ResponseEntity<ArchiveGroup> getGroup(@PathVariable Long groupId) {
        Optional<ArchiveGroup> group = repository.findByGroupId(groupId);
        return group.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/retry")
    public ResponseEntity<Map<String, String>> retryFailed() {
        archiveService.retryFailed();
        return ResponseEntity.ok(Map.of("status", "retry triggered"));
    }

    @PostMapping("/groups/{groupId}/retry")
    public ResponseEntity<Map<String, String>> retryGroup(@PathVariable Long groupId) {
        Optional<ArchiveGroup> group = repository.findByGroupId(groupId);
        if (group.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        archiveService.retryGroup(group.get());
        return ResponseEntity.ok(Map.of("status", "retry triggered for group " + groupId));
    }

    @DeleteMapping("/groups")
    public ResponseEntity<Map<String, String>> deleteAllGroups() {
        repository.deleteAll();
        return ResponseEntity.ok(Map.of("status", "all groups deleted"));
    }
}
