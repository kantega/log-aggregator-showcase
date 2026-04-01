package no.kantega.logmanager.repository;

import no.kantega.logmanager.model.LogEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LogEntryRepository extends JpaRepository<LogEntry, Long> {
    List<LogEntry> findByGroupIdOrderByCreatedAtAsc(Long groupId);
}
