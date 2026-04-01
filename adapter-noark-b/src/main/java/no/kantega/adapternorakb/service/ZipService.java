package no.kantega.adapternorakb.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.kantega.adapternorakb.model.ArchiveRequest;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class ZipService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public byte[] createZip(ArchiveRequest request) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            // Add metadata.json
            Map<String, Object> metadata = new LinkedHashMap<>();
            metadata.put("groupId", request.getGroupId());
            metadata.put("groupName", request.getGroupName());
            metadata.put("entryCount", request.getEntries().size());

            ZipEntry metadataEntry = new ZipEntry("metadata.json");
            zos.putNextEntry(metadataEntry);
            zos.write(objectMapper.writeValueAsBytes(metadata));
            zos.closeEntry();

            // Add each log entry as a separate text file
            for (ArchiveRequest.LogEntry entry : request.getEntries()) {
                ZipEntry entryFile = new ZipEntry("entry-" + entry.getEntryId() + ".txt");
                zos.putNextEntry(entryFile);
                zos.write(entry.getContent().getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();
            }
        }
        return baos.toByteArray();
    }
}
