package no.kantega.edge.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdapterConfig {
    private String name;
    private String url;
    private TriggerType trigger;

    public enum TriggerType {
        ON_ENTRY,
        ON_GROUP_CLOSE
    }
}
