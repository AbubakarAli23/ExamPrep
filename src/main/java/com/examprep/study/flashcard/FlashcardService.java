package com.examprep.study.flashcard;

import com.examprep.common.exception.ResourceNotFoundException;
import com.examprep.study.domain.FlashcardReview;
import com.examprep.study.domain.StudyPackage;
import com.examprep.study.dto.StudyPackDtos.FlashcardView;
import com.examprep.study.repository.FlashcardRepository;
import com.examprep.study.repository.FlashcardReviewRepository;
import com.examprep.study.repository.StudyPackageRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FlashcardService {

    private final StudyPackageRepository studyPackageRepository;
    private final FlashcardRepository flashcardRepository;
    private final FlashcardReviewRepository flashcardReviewRepository;

    public FlashcardService(StudyPackageRepository studyPackageRepository,
                            FlashcardRepository flashcardRepository,
                            FlashcardReviewRepository flashcardReviewRepository) {
        this.studyPackageRepository = studyPackageRepository;
        this.flashcardRepository = flashcardRepository;
        this.flashcardReviewRepository = flashcardReviewRepository;
    }

    @Transactional(readOnly = true)
    public List<FlashcardView> flashcards(UUID studyPackId, UUID ownerId) {
        requirePackage(studyPackId, ownerId);
        return flashcardRepository.findByStudyPackageIdOrderByPositionAsc(studyPackId).stream()
                .map(card -> new FlashcardView(
                        card.getId(), card.getPosition(), card.getFront(), card.getBack(), card.getSourceReference()))
                .toList();
    }

    @Transactional
    public void recordReview(UUID studyPackId, UUID ownerId, int reviewedCount) {
        requirePackage(studyPackId, ownerId);
        int count = Math.max(1, Math.min(200, reviewedCount));
        flashcardReviewRepository.save(new FlashcardReview(
                UUID.randomUUID(), ownerId, studyPackId, count, Instant.now()));
    }

    private StudyPackage requirePackage(UUID studyPackId, UUID ownerId) {
        return studyPackageRepository.findByIdAndOwnerId(studyPackId, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("That study package could not be found."));
    }
}
