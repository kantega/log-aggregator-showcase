package no.kantega.externalapismock.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReceivedRequest {
    private String endpoint;
    private String method;
    private String path;
    private String body;
    private Instant timestamp;
}
