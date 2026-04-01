package no.kantega.edge.repository;

import no.kantega.edge.model.ArchiveGroup;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface ArchiveGroupRepository extends MongoRepository<ArchiveGroup, String> {
    Optional<ArchiveGroup> findByGroupId(Long groupId);
    List<ArchiveGroup> findByStatus(ArchiveGroup.ArchiveStatus status);
}
