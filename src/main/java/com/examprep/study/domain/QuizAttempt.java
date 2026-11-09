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
@Table(name = "quiz_attempts")
public class QuizAttempt {

    @Id
    private UUID id;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(name = "study_package_id", nullable = false)
    private UUID studyPackageId;

    @Enumerated(EnumType.STRING)
    @Column(name = "question_section", nullable = false, length = 20)
    private QuestionSection questionSection;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AttemptStatus status;

    @Column(name = "total_questions", nullable = false)
    private int totalQuestions;

    @Column(name = "gradable_questions", nullable = false)
    private int gradableQuestions;

    @Column(name = "correct_count")
    private Integer correctCount;

    @Column(name = "score_percent")
    private Integer scorePercent;

    @Column(name = "allowed_seconds", nullable = false)
    private int allowedSeconds;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<AttemptAnswer> answers;

    protected QuizAttempt() {
    }

    public QuizAttempt(UUID id, UUID ownerId, UUID studyPackageId, QuestionSection questionSection,
                       int totalQuestions, int gradableQuestions, int allowedSeconds, Instant startedAt) {
        this.id = id;
        this.ownerId = ownerId;
        this.studyPackageId = studyPackageId;
        this.questionSection = questionSection;
        this.totalQuestions = totalQuestions;
        this.gradableQuestions = gradableQuestions;
        this.allowedSeconds = allowedSeconds;
        this.startedAt = startedAt;
        this.status = AttemptStatus.IN_PROGRESS;
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

    public QuestionSection getQuestionSection() {
        return questionSection;
    }

    public AttemptStatus getStatus() {
        return status;
    }

    public void setStatus(AttemptStatus status) {
        this.status = status;
    }

    public int getTotalQuestions() {
        return totalQuestions;
    }

    public int getGradableQuestions() {
        return gradableQuestions;
    }

    public Integer getCorrectCount() {
        return correctCount;
    }

    public void setCorrectCount(Integer correctCount) {
        this.correctCount = correctCount;
    }

    public Integer getScorePercent() {
        return scorePercent;
    }

    public void setScorePercent(Integer scorePercent) {
        this.scorePercent = scorePercent;
    }

    public int getAllowedSeconds() {
        return allowedSeconds;
    }

    public Integer getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(Integer durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(Instant submittedAt) {
        this.submittedAt = submittedAt;
    }

    public List<AttemptAnswer> getAnswers() {
        return answers;
    }

    public void setAnswers(List<AttemptAnswer> answers) {
        this.answers = answers;
    }
}
