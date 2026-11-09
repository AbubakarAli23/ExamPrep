package com.examprep.study.domain;

import com.examprep.study.domain.content.StudyContent.Difficulty;
import com.examprep.study.domain.content.StudyContent.QuestionType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * A generated question. The correct answer lives only on the server; the exam view never
 * includes it.
 */
@Entity
@Table(name = "quiz_questions")
public class QuizQuestion {

    @Id
    private UUID id;

    @Column(name = "study_package_id", nullable = false)
    private UUID studyPackageId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private QuestionSection section;

    @Column(nullable = false)
    private int position;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private QuestionType type;

    @Column(nullable = false, columnDefinition = "text")
    private String prompt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<String> options;

    @Column(name = "correct_answer", nullable = false, columnDefinition = "text")
    private String correctAnswer;

    @Column(columnDefinition = "text")
    private String explanation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Difficulty difficulty;

    @Column(name = "source_reference", length = 160)
    private String sourceReference;

    protected QuizQuestion() {
    }

    public QuizQuestion(UUID id, UUID studyPackageId, QuestionSection section, int position,
                        QuestionType type, String prompt, List<String> options, String correctAnswer,
                        String explanation, Difficulty difficulty, String sourceReference) {
        this.id = id;
        this.studyPackageId = studyPackageId;
        this.section = section;
        this.position = position;
        this.type = type;
        this.prompt = prompt;
        this.options = options;
        this.correctAnswer = correctAnswer;
        this.explanation = explanation;
        this.difficulty = difficulty;
        this.sourceReference = sourceReference;
    }

    public UUID getId() {
        return id;
    }

    public UUID getStudyPackageId() {
        return studyPackageId;
    }

    public QuestionSection getSection() {
        return section;
    }

    public int getPosition() {
        return position;
    }

    public QuestionType getType() {
        return type;
    }

    public String getPrompt() {
        return prompt;
    }

    public List<String> getOptions() {
        return options;
    }

    public String getCorrectAnswer() {
        return correctAnswer;
    }

    public String getExplanation() {
        return explanation;
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public String getSourceReference() {
        return sourceReference;
    }
}
