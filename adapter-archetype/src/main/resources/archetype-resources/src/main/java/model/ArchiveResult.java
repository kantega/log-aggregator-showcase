package ${package}.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ArchiveResult {
    private boolean success;
    private String message;
    private String adapter = "${providerSlug}";

    public static ArchiveResult ok() {
        return new ArchiveResult(true, "Archived successfully", "${providerSlug}");
    }

    public static ArchiveResult fail(String message) {
        return new ArchiveResult(false, message, "${providerSlug}");
    }
}
