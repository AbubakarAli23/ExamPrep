package com.examprep.study.repository;

import com.examprep.study.domain.AttemptStatus;
import com.examprep.study.domain.QuizAttempt;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, UUID> {

    Optional<QuizAttempt> findByIdAndOwnerId(UUID id, UUID ownerId);

    List<QuizAttempt> findByOwnerIdAndStatusOrderBySubmittedAtDesc(UUID ownerId, AttemptStatus status);

    List<QuizAttempt> findTop5ByOwnerIdAndStatusOrderBySubmittedAtDesc(UUID ownerId, AttemptStatus status);

    long countByOwnerIdAndStatus(UUID ownerId, AttemptStatus status);

    List<QuizAttempt> findByStudyPackageIdAndOwnerId(UUID studyPackageId, UUID ownerId);
}
