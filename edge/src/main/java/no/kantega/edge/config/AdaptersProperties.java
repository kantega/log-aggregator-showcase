package no.kantega.edge.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "")
public class AdaptersProperties {
    private List<AdapterConfig> adapters = new ArrayList<>();
}
