package com.examprep.study.repository;

import com.examprep.study.domain.Flashcard;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FlashcardRepository extends JpaRepository<Flashcard, UUID> {

    List<Flashcard> findByStudyPackageIdOrderByPositionAsc(UUID studyPackageId);

    long countByStudyPackageId(UUID studyPackageId);

    void deleteByStudyPackageId(UUID studyPackageId);
}
