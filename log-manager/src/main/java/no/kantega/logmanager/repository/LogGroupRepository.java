package no.kantega.logmanager.repository;

import no.kantega.logmanager.model.LogGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LogGroupRepository extends JpaRepository<LogGroup, Long> {
    List<LogGroup> findAllByOrderByCreatedAtDesc();
}
