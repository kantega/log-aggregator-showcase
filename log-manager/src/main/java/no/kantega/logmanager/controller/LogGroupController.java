package no.kantega.logmanager.controller;

import no.kantega.logmanager.model.LogEntry;
import no.kantega.logmanager.model.LogGroup;
import no.kantega.logmanager.service.LogManagerService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/groups")
public class LogGroupController {

    private final LogManagerService logManagerService;

    public LogGroupController(LogManagerService logManagerService) {
        this.logManagerService = logManagerService;
    }

    @PostMapping
    public ResponseEntity<LogGroup> createGroup(@RequestBody Map<String, String> body) {
        LogGroup group = logManagerService.createGroup(body.get("name"));
        return ResponseEntity.status(HttpStatus.CREATED).body(group);
    }

    @GetMapping
    public ResponseEntity<List<LogGroup>> getAllGroups() {
        return ResponseEntity.ok(logManagerService.getAllGroups());
    }

    @GetMapping("/{id}")
    public ResponseEntity<LogGroup> getGroup(@PathVariable Long id) {
        return ResponseEntity.ok(logManagerService.getGroup(id));
    }

    @GetMapping("/{id}/entries")
    public ResponseEntity<List<LogEntry>> getEntries(@PathVariable Long id) {
        return ResponseEntity.ok(logManagerService.getEntriesForGroup(id));
    }

    @PostMapping("/{id}/entries")
    public ResponseEntity<LogEntry> addEntry(@PathVariable Long id, @RequestBody Map<String, String> body) {
        LogEntry entry = logManagerService.addEntry(id, body.get("content"));
        return ResponseEntity.status(HttpStatus.CREATED).body(entry);
    }

    @PostMapping("/{id}/close")
    public ResponseEntity<LogGroup> closeGroup(@PathVariable Long id) {
        return ResponseEntity.ok(logManagerService.closeGroup(id));
    }
}
