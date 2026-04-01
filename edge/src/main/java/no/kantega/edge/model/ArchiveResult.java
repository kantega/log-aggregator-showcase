package no.kantega.edge.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ArchiveResult {
    private boolean success;
    private String message;
    private String adapter;
}
