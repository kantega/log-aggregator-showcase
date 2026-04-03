package no.kantega.adapternorakb.service;

import no.kantega.adapternorakb.model.ArchiveRequest;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;

class ZipServiceTest {

    private final ZipService zipService = new ZipService();

    @Test
    void createZip_containsMetadataAndEntries() throws IOException {
        ArchiveRequest request = new ArchiveRequest("GROUP_CLOSED", 1L, "Test Group", List.of(
                new ArchiveRequest.LogEntry(10L, "first entry", "2024-01-01T00:00:00Z"),
                new ArchiveRequest.LogEntry(20L, "second entry", "2024-01-02T00:00:00Z")
        ));

        byte[] zip = zipService.createZip(request);
        List<String> entryNames = getZipEntryNames(zip);

        assertThat(entryNames).containsExactly("metadata.json", "entry-10.txt", "entry-20.txt");
    }

    @Test
    void createZip_metadataContainsGroupInfo() throws IOException {
        ArchiveRequest request = new ArchiveRequest("GROUP_CLOSED", 42L, "My Group", List.of(
                new ArchiveRequest.LogEntry(1L, "content", "2024-01-01T00:00:00Z")
        ));

        byte[] zip = zipService.createZip(request);
        String metadata = readZipEntry(zip, "metadata.json");

        assertThat(metadata).contains("\"groupId\":42");
        assertThat(metadata).contains("\"groupName\":\"My Group\"");
        assertThat(metadata).contains("\"entryCount\":1");
    }

    @Test
    void createZip_entryContentIsCorrect() throws IOException {
        ArchiveRequest request = new ArchiveRequest("GROUP_CLOSED", 1L, "Group", List.of(
                new ArchiveRequest.LogEntry(10L, "hello world", "2024-01-01T00:00:00Z")
        ));

        byte[] zip = zipService.createZip(request);
        String content = readZipEntry(zip, "entry-10.txt");

        assertThat(content).isEqualTo("hello world");
    }

    @Test
    void createZip_emptyEntries() throws IOException {
        ArchiveRequest request = new ArchiveRequest("GROUP_CLOSED", 1L, "Empty", List.of());
        byte[] zip = zipService.createZip(request);
        List<String> entryNames = getZipEntryNames(zip);

        assertThat(entryNames).containsExactly("metadata.json");
    }

    private List<String> getZipEntryNames(byte[] zip) throws IOException {
        List<String> names = new ArrayList<>();
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zip))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                names.add(entry.getName());
            }
        }
        return names;
    }

    private String readZipEntry(byte[] zip, String entryName) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zip))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.getName().equals(entryName)) {
                    return new String(zis.readAllBytes());
                }
            }
        }
        throw new IllegalArgumentException("Entry not found: " + entryName);
    }
}
