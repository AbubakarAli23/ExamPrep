package com.examprep.study.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "flashcards")
public class Flashcard {

    @Id
    private UUID id;

    @Column(name = "study_package_id", nullable = false)
    private UUID studyPackageId;

    @Column(nullable = false)
    private int position;

    @Column(nullable = false, columnDefinition = "text")
    private String front;

    @Column(nullable = false, columnDefinition = "text")
    private String back;

    @Column(name = "source_reference", length = 160)
    private String sourceReference;

    protected Flashcard() {
    }

    public Flashcard(UUID id, UUID studyPackageId, int position, String front, String back,
                     String sourceReference) {
        this.id = id;
        this.studyPackageId = studyPackageId;
        this.position = position;
        this.front = front;
        this.back = back;
        this.sourceReference = sourceReference;
    }

    public UUID getId() {
        return id;
    }

    public UUID getStudyPackageId() {
        return studyPackageId;
    }

    public int getPosition() {
        return position;
    }

    public String getFront() {
        return front;
    }

    public String getBack() {
        return back;
    }

    public String getSourceReference() {
        return sourceReference;
    }
}
