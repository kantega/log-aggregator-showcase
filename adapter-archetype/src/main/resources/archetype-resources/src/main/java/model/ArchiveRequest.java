package ${package}.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Incoming archive request from Edge. The shape is identical across all adapters —
 * do not change it here. Provider-specific shape goes in {@link ${providerName}Payload}.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ArchiveRequest {
    private String eventType;
    private Long groupId;
    private String groupName;
    private List<LogEntry> entries;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LogEntry {
        private Long entryId;
        private String content;
        private String timestamp;
    }
}
