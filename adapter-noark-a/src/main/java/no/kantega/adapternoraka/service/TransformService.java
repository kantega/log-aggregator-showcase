package no.kantega.adapternoraka.service;

import no.kantega.adapternoraka.model.ArchiveRequest;
import no.kantega.adapternoraka.model.NoarkAPayload;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TransformService {

    public NoarkAPayload transform(ArchiveRequest request) {
        List<NoarkAPayload.Document> documents = request.getEntries().stream()
                .map(entry -> new NoarkAPayload.Document(
                        "entry-" + entry.getEntryId(),
                        "Log Entry #" + entry.getEntryId(),
                        entry.getContent(),
                        entry.getTimestamp()
                ))
                .collect(Collectors.toList());

        return new NoarkAPayload(
                request.getEventType(),
                request.getGroupName(),
                "Log group " + request.getGroupId() + " - " + request.getGroupName(),
                Instant.now().toString(),
                documents
        );
    }
}
