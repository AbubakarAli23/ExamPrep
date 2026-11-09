package com.examprep.study.domain;

import com.examprep.study.domain.content.StudyContent;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * A generated study package and its processing state. Learning content is stored as typed
 * JSONB; flashcards, questions, attempts and tutor messages are relational.
 */
@Entity
@Table(name = "study_packages")
public class StudyPackage {

    @Id
    private UUID id;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(name = "document_id", nullable = false)
    private UUID documentId;

    @Column(length = 300)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private StudyPackageStatus status;

    @Column(name = "progress_message", length = 300)
    private String progressMessage;

    @Column(length = 40)
    private String language;

    @Column(name = "academic_level", length = 60)
    private String academicLevel;

    @Column(name = "exam_date")
    private LocalDate examDate;

    @Column(name = "requested_question_count", nullable = false)
    private int requestedQuestionCount;

    @Column(name = "requested_mock_question_count", nullable = false)
    private int requestedMockQuestionCount;

    @Column(name = "requested_flashcard_count", nullable = false)
    private int requestedFlashcardCount;

    @Column(name = "document_intro", columnDefinition = "text")
    private String introduction;

    @Column(name = "overall_summary", columnDefinition = "text")
    private String overallSummary;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "learning_objectives", columnDefinition = "jsonb")
    private List<String> learningObjectives;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "revision_priorities", columnDefinition = "jsonb")
    private List<String> revisionPriorities;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<StudyContent.Topic> topics;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "revision_notes", columnDefinition = "jsonb")
    private List<StudyContent.NoteSection> revisionNotes;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<StudyContent.Definition> definitions;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<StudyContent.Formula> formulas;

    @Column(name = "failure_reason", length = 1000)
    private String failureReason;

    @Column(name = "ai_model", length = 120)
    private String aiModel;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    protected StudyPackage() {
    }

    public StudyPackage(UUID id, UUID ownerId, UUID documentId, String language, String academicLevel,
                        LocalDate examDate, int requestedQuestionCount, int requestedMockQuestionCount,
                        int requestedFlashcardCount) {
        this.id = id;
        this.ownerId = ownerId;
        this.documentId = documentId;
        this.language = language;
        this.academicLevel = academicLevel;
        this.examDate = examDate;
        this.requestedQuestionCount = requestedQuestionCount;
        this.requestedMockQuestionCount = requestedMockQuestionCount;
        this.requestedFlashcardCount = requestedFlashcardCount;
        this.status = StudyPackageStatus.QUEUED;
        this.progressMessage = "Queued for generation.";
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public UUID getDocumentId() {
        return documentId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public StudyPackageStatus getStatus() {
        return status;
    }

    public void setStatus(StudyPackageStatus status) {
        this.status = status;
    }

    public String getProgressMessage() {
        return progressMessage;
    }

    public void setProgressMessage(String progressMessage) {
        this.progressMessage = progressMessage;
    }

    public String getLanguage() {
        return language;
    }

    public String getAcademicLevel() {
        return academicLevel;
    }

    public LocalDate getExamDate() {
        return examDate;
    }

    public int getRequestedQuestionCount() {
        return requestedQuestionCount;
    }

    public int getRequestedFlashcardCount() {
        return requestedFlashcardCount;
    }

    public int getRequestedMockQuestionCount() {
        return requestedMockQuestionCount;
    }

    public String getIntroduction() {
        return introduction;
    }

    public void setIntroduction(String introduction) {
        this.introduction = introduction;
    }

    public String getOverallSummary() {
        return overallSummary;
    }

    public void setOverallSummary(String overallSummary) {
        this.overallSummary = overallSummary;
    }

    public List<String> getLearningObjectives() {
        return learningObjectives;
    }

    public void setLearningObjectives(List<String> learningObjectives) {
        this.learningObjectives = learningObjectives;
    }

    public List<String> getRevisionPriorities() {
        return revisionPriorities;
    }

    public void setRevisionPriorities(List<String> revisionPriorities) {
        this.revisionPriorities = revisionPriorities;
    }

    public List<StudyContent.Topic> getTopics() {
        return topics;
    }

    public void setTopics(List<StudyContent.Topic> topics) {
        this.topics = topics;
    }

    public List<StudyContent.NoteSection> getRevisionNotes() {
        return revisionNotes;
    }

    public void setRevisionNotes(List<StudyContent.NoteSection> revisionNotes) {
        this.revisionNotes = revisionNotes;
    }

    public List<StudyContent.Definition> getDefinitions() {
        return definitions;
    }

    public void setDefinitions(List<StudyContent.Definition> definitions) {
        this.definitions = definitions;
    }

    public List<StudyContent.Formula> getFormulas() {
        return formulas;
    }

    public void setFormulas(List<StudyContent.Formula> formulas) {
        this.formulas = formulas;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public String getAiModel() {
        return aiModel;
    }

    public void setAiModel(String aiModel) {
        this.aiModel = aiModel;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }
}
