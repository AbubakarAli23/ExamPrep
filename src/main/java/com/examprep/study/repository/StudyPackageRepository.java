package com.examprep.study.repository;

import com.examprep.study.domain.StudyPackage;
import com.examprep.study.domain.StudyPackageStatus;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudyPackageRepository extends JpaRepository<StudyPackage, UUID> {

    Optional<StudyPackage> findByIdAndOwnerId(UUID id, UUID ownerId);

    List<StudyPackage> findByOwnerIdOrderByCreatedAtDesc(UUID ownerId);

    List<StudyPackage> findTop5ByOwnerIdOrderByCreatedAtDesc(UUID ownerId);

    List<StudyPackage> findByStatusInAndUpdatedAtBefore(Collection<StudyPackageStatus> statuses, Instant cutoff);

    void deleteByDocumentId(UUID documentId);

    long countByOwnerId(UUID ownerId);
}
