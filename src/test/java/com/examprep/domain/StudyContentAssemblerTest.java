package com.examprep.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.examprep.ai.dto.AiStudyContent;
import com.examprep.ai.exception.AiResponseException;
import com.examprep.ai.service.StudyContentAssembler;
import com.examprep.ai.validation.CitationValidator;
import com.examprep.study.domain.content.StudyContent;
import com.examprep.study.dto.StudyPreferences;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class StudyContentAssemblerTest {

    private final StudyContentAssembler assembler = new StudyContentAssembler();
    private final Set<String> allowed = CitationValidator.allowedReferences(List.of("Page 1", "Page 2"));
    private final StudyPreferences preferences = StudyPreferences.defaults();

    @Test
    void buildsAValidatedPackageAndDiscardsInvalidCitations() {
        AiStudyContent raw = validContent();

        StudyContent.GeneratedPackage generated =
                assembler.assemble(raw, allowed, preferences, "Document title");

        assertEquals("Cell biology", generated.title());
        assertEquals("Page 1", generated.topics().get(0).sourceReferences().get(0));
        assertNull(generated.definitions().get(0).sourceReference(), "Invalid citation should be dropped");
        assertEquals(3, generated.practiceQuestions().size());
        assertEquals(StudyContent.QuestionType.MULTIPLE_CHOICE, generated.practiceQuestions().get(0).type());
        assertEquals(StudyContent.Difficulty.EASY, generated.practiceQuestions().get(0).difficulty());
        assertEquals(List.of("True", "False"), generated.practiceQuestions().get(1).options());
        assertEquals("True", generated.practiceQuestions().get(1).correctAnswer());
    }

    @Test
    void dropsMultipleChoiceQuestionsWhoseAnswerIsNotAnOption() {
        AiStudyContent raw = withPracticeQuestion(new AiStudyContent.AiQuestion(
                "MULTIPLE_CHOICE", "Which option is right?", List.of("A", "B", "C", "D"),
                "E", "Explanation", "EASY", "Page 1"));

        StudyContent.GeneratedPackage generated = assembler.assemble(raw, allowed, preferences, "Title");

        assertEquals(3, generated.practiceQuestions().size());
        assertTrue(generated.practiceQuestions().stream()
                .noneMatch(question -> question.prompt().contains("Which option is right?")));
    }

    @Test
    void normalisesTrueFalseAnswersAndAcceptsNumericOptionIndexes() {
        AiStudyContent raw = withPracticeQuestion(new AiStudyContent.AiQuestion(
                "TRUE_FALSE", "Is water polar?", List.of(), "true", "Yes.", "easy", "Page 2"));

        StudyContent.GeneratedPackage generated = assembler.assemble(raw, allowed, preferences, "Title");
        StudyContent.GeneratedQuestion question = generated.practiceQuestions().get(3);

        assertEquals(StudyContent.QuestionType.TRUE_FALSE, question.type());
        assertEquals("True", question.correctAnswer());
        assertEquals(List.of("True", "False"), question.options());
    }

    @Test
    void rejectsPackagesThatAreTooIncompleteToBeUseful() {
        AiStudyContent empty = new AiStudyContent("Title", "Intro", "Summary",
                List.of("Objective"), List.of("Priority"),
                Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
                Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
                Collections.emptyList());

        assertThrows(AiResponseException.class, () -> assembler.assemble(empty, allowed, preferences, "Title"));
    }

    private AiStudyContent withPracticeQuestion(AiStudyContent.AiQuestion extra) {
        List<AiStudyContent.AiQuestion> practice = new java.util.ArrayList<>(validContent().practiceQuestions());
        practice.add(extra);
        return new AiStudyContent("Cell biology", "Intro", "Summary",
                List.of("Objective one", "Objective two"), List.of("Priority"),
                validContent().topics(), validContent().revisionNotes(), validContent().definitions(),
                validContent().formulas(), validContent().flashcards(), practice, validContent().mockQuestions());
    }

    private AiStudyContent validContent() {
        return new AiStudyContent(
                "Cell biology",
                "This document covers the cell membrane.",
                "Membranes are selectively permeable and transport can be passive or active.",
                List.of("Explain transport", "Compare diffusion and osmosis"),
                List.of("Membrane proteins"),
                List.of(new AiStudyContent.AiTopic("Transport", "Passive and active transport.",
                        List.of("Diffusion", "Osmosis"), List.of("Page 1", "Page 99"))),
                List.of(new AiStudyContent.AiNote("Diffusion", "Diffusion moves particles down a gradient.",
                        List.of("Page 1"))),
                List.of(new AiStudyContent.AiDefinition("Osmosis", "Water movement across a membrane.",
                        "Page 99")),
                List.of(new AiStudyContent.AiFormula("Water potential", "psi = psi_s + psi_p",
                        "Water moves to lower potential.", "Page 2")),
                List.of(
                        new AiStudyContent.AiFlashcard("What is diffusion?", "Down-gradient movement.", "Page 1"),
                        new AiStudyContent.AiFlashcard("What is osmosis?", "Water movement.", "Page 1"),
                        new AiStudyContent.AiFlashcard("What powers active transport?", "ATP.", "Page 2")),
                List.of(
                        new AiStudyContent.AiQuestion("MULTIPLE_CHOICE", "Which needs ATP?",
                                List.of("Diffusion", "Osmosis", "Active transport", "Facilitated diffusion"),
                                "Active transport", "Needs ATP.", "EASY", "Page 2"),
                        new AiStudyContent.AiQuestion("TRUE_FALSE", "Osmosis is diffusion of water.",
                                List.of("True", "False"), "True", "Correct.", "MEDIUM", "Page 1"),
                        new AiStudyContent.AiQuestion("SHORT_ANSWER", "Define passive transport.",
                                List.of(), "Movement without energy.", "Passive.", "HARD", "Page 1")),
                List.of(
                        new AiStudyContent.AiQuestion("MULTIPLE_CHOICE", "Which process uses ATP?",
                                List.of("Diffusion", "Active transport", "Osmosis", "Filtration"),
                                "Active transport", "Explanation.", "EASY", "Page 2"),
                        new AiStudyContent.AiQuestion("TRUE_FALSE", "Diffusion needs ATP.",
                                List.of("True", "False"), "False", "It is passive.", "MEDIUM", "Page 1"),
                        new AiStudyContent.AiQuestion("SHORT_ANSWER", "Define osmosis.",
                                List.of(), "Water movement.", "Osmosis.", "MEDIUM", "Page 1")));
    }
}
