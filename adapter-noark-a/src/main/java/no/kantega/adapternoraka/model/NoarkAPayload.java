package no.kantega.adapternoraka.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NoarkAPayload {
    private String eventType;
    private String title;
    private String description;
    private String archiveDate;
    private List<Document> documents;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Document {
        private String documentId;
        private String title;
        private String content;
        private String createdDate;
    }
}
