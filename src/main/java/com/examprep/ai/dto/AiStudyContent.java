package com.examprep.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/**
 * Raw structured output from Gemini. Deliberately permissive (strings, nullable lists) so
 * that unpredictable model output can be validated instead of crashing deserialization.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AiStudyContent(
        String title,
        String introduction,
        String overallSummary,
        List<String> learningObjectives,
        List<String> revisionPriorities,
        List<AiTopic> topics,
        List<AiNote> revisionNotes,
        List<AiDefinition> definitions,
        List<AiFormula> formulas,
        List<AiFlashcard> flashcards,
        List<AiQuestion> practiceQuestions,
        List<AiQuestion> mockQuestions) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AiTopic(String name, String explanation, List<String> keyConcepts,
                          List<String> sourceReferences) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AiNote(String heading, String body, List<String> sourceReferences) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AiDefinition(String term, String definition, String sourceReference) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AiFormula(String name, String expression, String explanation, String sourceReference) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AiFlashcard(String front, String back, String sourceReference) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AiQuestion(String type, String prompt, List<String> options, String correctAnswer,
                             String explanation, String difficulty, String sourceReference) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AiSectionSummary(String summary, List<String> keyPoints) {
    }
}
