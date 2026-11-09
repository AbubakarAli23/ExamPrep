package com.examprep.study.domain.content;

import java.util.List;

/**
 * Typed learning content. These records are stored as JSONB for display, while flashcards,
 * questions and attempts use relational tables for querying and scoring.
 */
public final class StudyContent {

    private StudyContent() {
    }

    public record Topic(
            String name,
            String explanation,
            List<String> keyConcepts,
            List<String> sourceReferences) {
    }

    public record NoteSection(
            String heading,
            String body,
            List<String> sourceReferences) {
    }

    public record Definition(
            String term,
            String definition,
            String sourceReference) {
    }

    public record Formula(
            String name,
            String expression,
            String explanation,
            String sourceReference) {
    }

    public record Flashcard(
            String front,
            String back,
            String sourceReference) {
    }

    public record GeneratedPackage(
            String title,
            String introduction,
            String overallSummary,
            List<String> learningObjectives,
            List<String> revisionPriorities,
            List<Topic> topics,
            List<NoteSection> revisionNotes,
            List<Definition> definitions,
            List<Formula> formulas,
            List<Flashcard> flashcards,
            List<GeneratedQuestion> practiceQuestions,
            List<GeneratedQuestion> mockQuestions) {
    }

    public enum QuestionType {
        MULTIPLE_CHOICE,
        TRUE_FALSE,
        SHORT_ANSWER
    }

    public enum Difficulty {
        EASY,
        MEDIUM,
        HARD
    }

    public record GeneratedQuestion(
            QuestionType type,
            String prompt,
            List<String> options,
            String correctAnswer,
            String explanation,
            Difficulty difficulty,
            String sourceReference) {
    }
}
