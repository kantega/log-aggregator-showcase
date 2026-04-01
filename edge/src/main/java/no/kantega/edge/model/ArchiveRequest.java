package no.kantega.edge.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ArchiveRequest {
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
