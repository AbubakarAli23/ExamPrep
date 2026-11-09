package com.examprep.study.repository;

import com.examprep.study.domain.FlashcardReview;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FlashcardReviewRepository extends JpaRepository<FlashcardReview, UUID> {

    List<FlashcardReview> findTop5ByOwnerIdOrderByReviewedAtDesc(UUID ownerId);

    long countByOwnerId(UUID ownerId);

    List<FlashcardReview> findByOwnerIdAndReviewedAtAfter(UUID ownerId, Instant after);

    @Query("select coalesce(sum(review.reviewedCount), 0) from FlashcardReview review where review.ownerId = :ownerId")
    long sumReviewedCount(@Param("ownerId") UUID ownerId);
}
