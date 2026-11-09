package com.examprep.study.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "tutor_messages")
public class TutorMessage {

    @Id
    private UUID id;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(name = "study_package_id", nullable = false)
    private UUID studyPackageId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private MessageRole role;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "source_references", columnDefinition = "jsonb")
    private List<String> sourceReferences;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected TutorMessage() {
    }

    public TutorMessage(UUID id, UUID ownerId, UUID studyPackageId, MessageRole role, String content,
                        List<String> sourceReferences, Instant createdAt) {
        this.id = id;
        this.ownerId = ownerId;
        this.studyPackageId = studyPackageId;
        this.role = role;
        this.content = content;
        this.sourceReferences = sourceReferences;
        this.createdAt = createdAt;
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

    public MessageRole getRole() {
        return role;
    }

    public String getContent() {
        return content;
    }

    public List<String> getSourceReferences() {
        return sourceReferences;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
