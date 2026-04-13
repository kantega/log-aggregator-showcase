package no.kantega.edge.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "archive_groups")
public class ArchiveGroup {

    @Id
    private String id;
    private Long groupId;
    private String name;
    private ArchiveStatus status;
    private List<LogEntryData> entries = new ArrayList<>();
    private List<ArchiveError> errors = new ArrayList<>();
    private List<ArchiveEvent> archiveEvents = new ArrayList<>();
    private int retryCount = 0;
    private Instant nextRetryAt;
    private Instant createdAt;
    private Instant updatedAt;

    public enum ArchiveStatus {
        PENDING, IN_PROGRESS, ARCHIVED, FAILED
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LogEntryData {
        private Long entryId;
        private String content;
        private String timestamp;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ArchiveError {
        private String adapter;
        private String eventType;
        private String message;
        private Instant timestamp;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ArchiveEvent {
        private String eventType;
        private String adapter;
        private EventStatus status;
        private String message;
        private Instant timestamp;
    }

    public enum EventStatus {
        SUCCESS, FAILED
    }
}
