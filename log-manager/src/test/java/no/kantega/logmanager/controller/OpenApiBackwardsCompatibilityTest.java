package no.kantega.logmanager.controller;

import no.kantega.logmanager.repository.LogEntryRepository;
import no.kantega.logmanager.repository.LogGroupRepository;
import org.junit.jupiter.api.Test;
import org.openapitools.openapidiff.core.OpenApiCompare;
import org.openapitools.openapidiff.core.model.ChangedOpenApi;
import org.openapitools.openapidiff.core.output.ConsoleRender;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@EnableAutoConfiguration(exclude = {
        DataSourceAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class,
        RabbitAutoConfiguration.class
})
class OpenApiBackwardsCompatibilityTest {

    private static final Path BASELINE_PATH = Paths.get("src/test/resources/openapi-baseline.json");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LogGroupRepository logGroupRepository;

    @MockitoBean
    private LogEntryRepository logEntryRepository;

    @MockitoBean
    private RabbitTemplate rabbitTemplate;

    @Test
    void validateBackwardsCompatibility() throws Exception {
        String currentSpec = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        if (!Files.exists(BASELINE_PATH)) {
            Files.createDirectories(BASELINE_PATH.getParent());
            Files.writeString(BASELINE_PATH, currentSpec);
            System.out.println("INFO: Created initial OpenAPI baseline at " + BASELINE_PATH);
            return;
        }

        String baselineSpec = Files.readString(BASELINE_PATH);
        ChangedOpenApi diff = OpenApiCompare.fromContents(baselineSpec, currentSpec);

        if (diff.isIncompatible()) {
            fail("Breaking API changes detected! Update the baseline if intentional.\n\n" + renderDiff(diff));
        }

        if (diff.isDifferent()) {
            System.out.println("INFO: Non-breaking API changes detected:\n" + renderDiff(diff));
        }
    }

    private String renderDiff(ChangedOpenApi diff) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            OutputStreamWriter writer = new OutputStreamWriter(baos, StandardCharsets.UTF_8);
            new ConsoleRender().render(diff, writer);
            writer.flush();
            return baos.toString(StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "Unable to render diff: " + e.getMessage();
        }
    }
}
