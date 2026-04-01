package no.kantega.edge.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LogEvent {
    @JsonAlias("eventType")
    private String event; // GROUP_CREATED, ENTRY_ADDED, GROUP_CLOSED
    private Long groupId;
    @JsonAlias("groupName")
    private String name;
    private Long entryId;
    @JsonAlias("entryContent")
    private String content;
    private String timestamp;
}
