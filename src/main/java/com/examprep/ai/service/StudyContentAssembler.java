package com.examprep.ai.service;

import com.examprep.ai.dto.AiStudyContent;
import com.examprep.ai.exception.AiResponseException;
import com.examprep.ai.validation.CitationValidator;
import com.examprep.study.domain.content.StudyContent;
import com.examprep.study.domain.content.StudyContent.Difficulty;
import com.examprep.study.domain.content.StudyContent.GeneratedQuestion;
import com.examprep.study.domain.content.StudyContent.QuestionType;
import com.examprep.study.dto.StudyPreferences;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Turns model output into validated domain content: drops malformed questions, strips
 * invalid citations, trims to requested counts and rejects packages that are too
 * incomplete to be useful.
 */
@Component
public class StudyContentAssembler {

    public StudyContent.GeneratedPackage assemble(AiStudyContent raw,
                                                  Set<String> allowedReferences,
                                                  StudyPreferences preferences,
                                                  String documentTitle) {
        if (raw == null) {
            throw new AiResponseException("The study package response was empty.");
        }
        String title = firstNonBlank(raw.title(), documentTitle, "Study package");
        String introduction = requireText(raw.introduction(), "introduction");
        String summary = requireText(raw.overallSummary(), "summary");

        List<StudyContent.Topic> topics = new ArrayList<>();
        if (raw.topics() != null) {
            for (AiStudyContent.AiTopic topic : raw.topics()) {
                if (topic == null || isBlank(topic.name()) || isBlank(topic.explanation())) {
                    continue;
                }
                topics.add(new StudyContent.Topic(
                        topic.name().strip(),
                        topic.explanation().strip(),
                        safeList(topic.keyConcepts(), 8),
                        citations(topic.sourceReferences(), allowedReferences, 3)));
            }
        }

        List<StudyContent.NoteSection> notes = new ArrayList<>();
        if (raw.revisionNotes() != null) {
            for (AiStudyContent.AiNote note : raw.revisionNotes()) {
                if (note == null || isBlank(note.heading()) || isBlank(note.body())) {
                    continue;
                }
                notes.add(new StudyContent.NoteSection(
                        note.heading().strip(),
                        note.body().strip(),
                        citations(note.sourceReferences(), allowedReferences, 3)));
            }
        }

        List<StudyContent.Definition> definitions = new ArrayList<>();
        if (raw.definitions() != null) {
            for (AiStudyContent.AiDefinition definition : raw.definitions()) {
                if (definition == null || isBlank(definition.term()) || isBlank(definition.definition())) {
                    continue;
                }
                definitions.add(new StudyContent.Definition(
                        definition.term().strip(),
                        definition.definition().strip(),
                        CitationValidator.firstValid(definition.sourceReference(), allowedReferences)));
            }
        }

        List<StudyContent.Formula> formulas = new ArrayList<>();
        if (raw.formulas() != null) {
            for (AiStudyContent.AiFormula formula : raw.formulas()) {
                if (formula == null || isBlank(formula.name()) || isBlank(formula.expression())) {
                    continue;
                }
                formulas.add(new StudyContent.Formula(
                        formula.name().strip(),
                        formula.expression().strip(),
                        isBlank(formula.explanation()) ? null : formula.explanation().strip(),
                        CitationValidator.firstValid(formula.sourceReference(), allowedReferences)));
            }
        }

        List<StudyContent.Flashcard> flashcards = new ArrayList<>();
        if (raw.flashcards() != null) {
            for (AiStudyContent.AiFlashcard flashcard : raw.flashcards()) {
                if (flashcard == null || isBlank(flashcard.front()) || isBlank(flashcard.back())) {
                    continue;
                }
                flashcards.add(new StudyContent.Flashcard(
                        flashcard.front().strip(),
                        flashcard.back().strip(),
                        CitationValidator.firstValid(flashcard.sourceReference(), allowedReferences)));
                if (flashcards.size() >= preferences.flashcardCount()) {
                    break;
                }
            }
        }

        List<GeneratedQuestion> practice = assembleQuestions(
                raw.practiceQuestions(), allowedReferences, preferences.practiceQuestionCount());
        List<GeneratedQuestion> mock = assembleQuestions(
                raw.mockQuestions(), allowedReferences, preferences.mockQuestionCount());

        if (topics.isEmpty()) {
            throw new AiResponseException("The generated package had no topics.");
        }
        if (notes.isEmpty()) {
            throw new AiResponseException("The generated package had no revision notes.");
        }
        if (practice.size() < 3) {
            throw new AiResponseException("The generated package did not contain enough practice questions.");
        }
        if (mock.size() < 3) {
            throw new AiResponseException("The generated package did not contain enough mock examination questions.");
        }
        if (flashcards.size() < 3) {
            throw new AiResponseException("The generated package did not contain enough flashcards.");
        }

        return new StudyContent.GeneratedPackage(
                title,
                introduction,
                summary,
                safeList(raw.learningObjectives(), 10),
                safeList(raw.revisionPriorities(), 8),
                topics,
                notes,
                definitions,
                formulas,
                flashcards,
                practice,
                mock);
    }

    private List<GeneratedQuestion> assembleQuestions(List<AiStudyContent.AiQuestion> rawQuestions,
                                                      Set<String> allowedReferences,
                                                      int limit) {
        List<GeneratedQuestion> questions = new ArrayList<>();
        if (rawQuestions == null) {
            return questions;
        }
        for (AiStudyContent.AiQuestion question : rawQuestions) {
            GeneratedQuestion generated = convert(question, allowedReferences);
            if (generated == null) {
                continue;
            }
            if (questions.stream().anyMatch(existing -> existing.prompt().equalsIgnoreCase(generated.prompt()))) {
                continue;
            }
            questions.add(generated);
            if (questions.size() >= limit) {
                break;
            }
        }
        return questions;
    }

    private GeneratedQuestion convert(AiStudyContent.AiQuestion raw, Set<String> allowedReferences) {
        if (raw == null || isBlank(raw.prompt()) || isBlank(raw.correctAnswer())) {
            return null;
        }
        QuestionType type = questionType(raw.type(), raw.options());
        if (type == null) {
            return null;
        }
        List<String> options = type == QuestionType.SHORT_ANSWER
                ? List.of()
                : new ArrayList<>(safeList(raw.options(), 8));
        String answer = raw.correctAnswer().strip();

        if (type == QuestionType.MULTIPLE_CHOICE) {
            if (options.size() < 2) {
                return null;
            }
            final String expected = answer;
            String matched = options.stream()
                    .filter(option -> option.strip().equalsIgnoreCase(expected))
                    .findFirst()
                    .orElse(null);
            if (matched == null) {
                int index = parseIndex(answer);
                if (index < 0 || index >= options.size()) {
                    return null;
                }
                matched = options.get(index);
            }
            answer = matched.strip();
        } else if (type == QuestionType.TRUE_FALSE) {
            String normalized = answer.toLowerCase(Locale.ROOT);
            if (!normalized.equals("true") && !normalized.equals("false")) {
                return null;
            }
            answer = normalized.equals("true") ? "True" : "False";
            options = List.of("True", "False");
        }

        return new GeneratedQuestion(
                type,
                raw.prompt().strip(),
                options,
                answer,
                isBlank(raw.explanation()) ? null : raw.explanation().strip(),
                difficulty(raw.difficulty()),
                CitationValidator.firstValid(raw.sourceReference(), allowedReferences));
    }

    private QuestionType questionType(String rawType, List<String> options) {
        if (!isBlank(rawType)) {
            String normalized = rawType.strip().toUpperCase(Locale.ROOT).replace(' ', '_').replace('-', '_');
            try {
                return QuestionType.valueOf(normalized);
            } catch (IllegalArgumentException ignored) {
                // Fall through to inference from options.
            }
        }
        if (options == null || options.isEmpty()) {
            return QuestionType.SHORT_ANSWER;
        }
        if (options.size() == 2) {
            String first = options.get(0).strip();
            String second = options.get(1).strip();
            if (first.equalsIgnoreCase("true") && second.equalsIgnoreCase("false")) {
                return QuestionType.TRUE_FALSE;
            }
        }
        return QuestionType.MULTIPLE_CHOICE;
    }

    private Difficulty difficulty(String raw) {
        if (!isBlank(raw)) {
            String normalized = raw.strip().toUpperCase(Locale.ROOT);
            if (normalized.equals("MODERATE")) {
                return Difficulty.MEDIUM;
            }
            try {
                return Difficulty.valueOf(normalized);
            } catch (IllegalArgumentException ignored) {
                // Default below.
            }
        }
        return Difficulty.MEDIUM;
    }

    private int parseIndex(String answer) {
        try {
            return Integer.parseInt(answer.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException ex) {
            return -1;
        }
    }

    private List<String> citations(List<String> references, Set<String> allowed, int limit) {
        List<String> valid = CitationValidator.filter(references, allowed);
        return valid.size() > limit ? new ArrayList<>(valid.subList(0, limit)) : valid;
    }

    private List<String> safeList(List<String> values, int limit) {
        if (values == null) {
            return List.of();
        }
        Set<String> cleaned = new LinkedHashSet<>();
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                cleaned.add(value.strip());
            }
            if (cleaned.size() >= limit) {
                break;
            }
        }
        return new ArrayList<>(cleaned);
    }

    private String requireText(String value, String label) {
        if (isBlank(value)) {
            throw new AiResponseException("The generated package was missing its " + label + ".");
        }
        return value.strip();
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.strip();
            }
        }
        return "Study package";
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
