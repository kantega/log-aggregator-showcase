package no.kantega.adapternorakb.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ArchiveResult {
    private boolean success;
    private String message;
    private String adapter = "noark-b";

    public static ArchiveResult ok() {
        return new ArchiveResult(true, "Archived successfully", "noark-b");
    }

    public static ArchiveResult fail(String message) {
        return new ArchiveResult(false, message, "noark-b");
    }
}
