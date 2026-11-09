package com.examprep.document.repository;

import com.examprep.document.domain.Document;
import com.examprep.document.domain.DocumentStatus;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentRepository extends JpaRepository<Document, UUID> {

    Optional<Document> findByIdAndOwnerId(UUID id, UUID ownerId);

    List<Document> findByOwnerIdOrderByCreatedAtDesc(UUID ownerId);

    List<Document> findTop5ByOwnerIdOrderByCreatedAtDesc(UUID ownerId);

    List<Document> findByStatusInAndUpdatedAtBefore(Collection<DocumentStatus> statuses, Instant cutoff);

    long countByOwnerId(UUID ownerId);
}
