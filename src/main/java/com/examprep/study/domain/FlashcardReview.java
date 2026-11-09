package com.examprep.study.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "flashcard_reviews")
public class FlashcardReview {

    @Id
    private UUID id;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(name = "study_package_id", nullable = false)
    private UUID studyPackageId;

    @Column(name = "reviewed_count", nullable = false)
    private int reviewedCount;

    @Column(name = "reviewed_at", nullable = false)
    private Instant reviewedAt;

    protected FlashcardReview() {
    }

    public FlashcardReview(UUID id, UUID ownerId, UUID studyPackageId, int reviewedCount, Instant reviewedAt) {
        this.id = id;
        this.ownerId = ownerId;
        this.studyPackageId = studyPackageId;
        this.reviewedCount = reviewedCount;
        this.reviewedAt = reviewedAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public UUID getStudyPackageId() {
        return studyPackageId;
    }

    public int getReviewedCount() {
        return reviewedCount;
    }

    public Instant getReviewedAt() {
        return reviewedAt;
    }
}
