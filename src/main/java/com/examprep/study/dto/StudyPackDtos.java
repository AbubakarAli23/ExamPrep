package com.examprep.study.dto;

import com.examprep.study.domain.content.StudyContent.Definition;
import com.examprep.study.domain.content.StudyContent.Formula;
import com.examprep.study.domain.content.StudyContent.NoteSection;
import com.examprep.study.domain.content.StudyContent.Topic;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * DTOs for study packages, exams and the tutor. Answer keys are deliberately absent from
 * the exam question view.
 */
public final class StudyPackDtos {

    private StudyPackDtos() {
    }

    public record StudyPackCreationResult(
            UUID studyPackId,
            String status,
            String message) {
    }

    public record StudyPackStatusDto(
            UUID studyPackId,
            UUID documentId,
            String status,
            int progressPercent,
            String message,
            String failureReason,
            boolean ready) {
    }

    public record StudyPackDto(
            UUID studyPackId,
            UUID documentId,
            String documentTitle,
            String title,
            String status,
            String introduction,
            String overallSummary,
            List<String> learningObjectives,
            List<String> revisionPriorities,
            List<Topic> topics,
            List<NoteSection> revisionNotes,
            List<Definition> definitions,
            List<Formula> formulas,
            long flashcardCount,
            long practiceQuestionCount,
            long mockQuestionCount,
            String language,
            String academicLevel,
            String examDateLabel,
            String createdAtLabel,
            String failureReason,
            String progressMessage,
            boolean ready,
            String model) {
    }

    public record StudyPackSummaryDto(
            UUID studyPackId,
            UUID documentId,
            String title,
            String documentTitle,
            String status,
            String createdAtLabel,
            long flashcardCount,
            long practiceQuestionCount,
            long mockQuestionCount,
            boolean ready) {
    }

    public record FlashcardView(
            UUID flashcardId,
            int position,
            String front,
            String back,
            String sourceReference) {
    }

    public record PracticeQuestionView(
            UUID questionId,
            String type,
            String prompt,
            List<String> options,
            String correctAnswer,
            String explanation,
            String difficulty,
            String sourceReference) {
    }

    public record ExamQuestionView(
            UUID questionId,
            int position,
            String type,
            String prompt,
            List<String> options,
            String difficulty,
            String sourceReference) {
    }

    public record ExamView(
            UUID attemptId,
            UUID studyPackId,
            String studyPackTitle,
            String status,
            int allowedSeconds,
            int totalQuestions,
            long startedAtEpochMillis,
            String startedAtLabel,
            List<ExamQuestionView> questions) {
    }

    /**
     * Submitted answers keyed by question id: the option index for multiple choice and
     * true/false, or free text for short answers.
     */
    public record QuizSubmissionDto(Map<String, String> answers) {
    }

    public record QuestionResult(
            UUID questionId,
            int position,
            String type,
            String prompt,
            List<String> options,
            String submittedAnswer,
            String correctAnswer,
            Boolean correct,
            String explanation,
            String difficulty,
            String sourceReference) {
    }

    public record QuizResultDto(
            UUID attemptId,
            UUID studyPackId,
            String studyPackTitle,
            int scorePercent,
            int correctCount,
            int gradableQuestions,
            int totalQuestions,
            int durationSeconds,
            String durationLabel,
            String submittedAtLabel,
            String feedbackHeadline,
            List<String> revisionAreas,
            List<QuestionResult> results) {
    }

    public record AttemptSummaryDto(
            UUID attemptId,
            UUID studyPackId,
            String studyPackTitle,
            int scorePercent,
            int correctCount,
            int gradableQuestions,
            String submittedAtLabel,
            String durationLabel) {
    }

    public record StudyProgressDto(
            int studyPackages,
            int completedExams,
            Integer averageScorePercent,
            Integer bestScorePercent,
            long flashcardsReviewed,
            int documents) {
    }

    public record TutorQuestionDto(String question) {
    }

    public record TutorAnswerDto(
            String answer,
            List<String> sourceReferences,
            boolean insufficientInformation,
            String createdAtLabel) {
    }

    public record TutorMessageView(
            String role,
            String content,
            List<String> sourceReferences,
            String createdAtLabel) {
    }

    public record TutorConversationDto(
            UUID studyPackId,
            String studyPackTitle,
            String status,
            List<TutorMessageView> messages) {
    }
}
