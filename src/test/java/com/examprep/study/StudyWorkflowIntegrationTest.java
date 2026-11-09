package com.examprep.study;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.examprep.ai.client.GeminiClient;
import com.examprep.ai.client.GeminiRequest;
import com.examprep.study.domain.MessageRole;
import com.examprep.study.domain.QuestionSection;
import com.examprep.study.domain.QuizAttempt;
import com.examprep.study.domain.QuizQuestion;
import com.examprep.study.domain.StudyPackage;
import com.examprep.study.domain.StudyPackageStatus;
import com.examprep.study.domain.TutorMessage;
import com.examprep.study.domain.content.StudyContent.QuestionType;
import com.examprep.study.repository.QuizAttemptRepository;
import com.examprep.study.repository.QuizQuestionRepository;
import com.examprep.study.repository.TutorMessageRepository;
import com.examprep.support.AiFixtures;
import com.examprep.support.AbstractIntegrationTest;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;

class StudyWorkflowIntegrationTest extends AbstractIntegrationTest {

    @MockitoBean
    private GeminiClient geminiClient;

    @Autowired
    private QuizQuestionRepository quizQuestionRepository;

    @Autowired
    private QuizAttemptRepository quizAttemptRepository;

    @Autowired
    private TutorMessageRepository tutorMessageRepository;

    @BeforeEach
    void stubGemini() {
        when(geminiClient.generate(any(GeminiRequest.class))).thenAnswer(invocation -> {
            GeminiRequest request = invocation.getArgument(0);
            return request.userPrompt().contains("<student_question>")
                    ? AiFixtures.tutorJson()
                    : AiFixtures.studyPackageJson();
        });
    }

    @Test
    void generationPersistsLearningContentAndRendersEveryPage() throws Exception {
        MockHttpSession session = registerAndSignIn(uniqueEmail());
        UUID documentId = createReadyDocument(session);
        UUID studyPackId = createStudyPackage(session, documentId);

        StudyPackage studyPackage = awaitStudyPackStatus(studyPackId, StudyPackageStatus.READY);
        assertEquals("Cell Membrane Transport", studyPackage.getTitle());
        assertNotNull(studyPackage.getOverallSummary());
        assertFalse(studyPackage.getTopics().isEmpty());
        assertFalse(studyPackage.getRevisionNotes().isEmpty());
        assertEquals(3, quizQuestionRepository
                .countByStudyPackageIdAndSection(studyPackId, QuestionSection.MOCK));

        String overview = page(session, "/study/" + studyPackId);
        assertEquals(true, overview.contains("Cell Membrane Transport"));

        for (String section : List.of("topics", "notes", "definitions", "formulas", "flashcards",
                "practice", "exam", "tutor")) {
            String html = page(session, "/study/" + studyPackId + "/" + section);
            assertTrue(html.contains("Study package"), "Page " + section + " did not render the study navigation");
        }

        String flashcards = page(session, "/study/" + studyPackId + "/flashcards");
        assertTrue(flashcards.contains("What is diffusion?"));
        String practice = page(session, "/study/" + studyPackId + "/practice");
        assertTrue(practice.contains("Show suggested answer"));
    }

    @Test
    void mockExamIsScoredOnTheServerAndAnswerKeyIsNotSentToTheBrowser() throws Exception {
        MockHttpSession session = registerAndSignIn(uniqueEmail());
        UUID documentId = createReadyDocument(session);
        UUID studyPackId = createStudyPackage(session, documentId);
        awaitStudyPackStatus(studyPackId, StudyPackageStatus.READY);

        MvcResult start = mockMvc.perform(post("/study/{id}/mock-exams", studyPackId)
                        .session(session)
                        .with(csrf())
                        .param("durationMinutes", "30"))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        String location = start.getResponse().getRedirectedUrl();
        assertNotNull(location);
        UUID attemptId = UUID.fromString(location.substring(location.lastIndexOf('/') + 1));

        String examPage = page(session, "/exams/" + attemptId);
        assertTrue(examPage.contains("Mock examination"));
        assertFalse(examPage.contains("Correct answer:"));
        assertFalse(examPage.contains("Active transport uses ATP to move"));

        List<QuizQuestion> questions = quizQuestionRepository
                .findByStudyPackageIdAndSectionOrderByPositionAsc(studyPackId, QuestionSection.MOCK);
        var submission = post("/exams/{id}/submit", attemptId).session(session).with(csrf());
        for (QuizQuestion question : questions) {
            if (question.getType() == QuestionType.SHORT_ANSWER) {
                submission.param("question-" + question.getId(), "My short answer");
            } else {
                int correctIndex = question.getOptions().indexOf(question.getCorrectAnswer());
                assertTrue(correctIndex >= 0, "Correct answer must be one of the options");
                submission.param("question-" + question.getId(), String.valueOf(correctIndex));
            }
        }

        mockMvc.perform(submission)
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/exams/" + attemptId + "/results"));

        QuizAttempt attempt = quizAttemptRepository.findById(attemptId).orElseThrow();
        assertEquals(100, attempt.getScorePercent());
        assertEquals(2, attempt.getGradableQuestions());
        assertEquals(2, attempt.getCorrectCount());

        String results = page(session, "/exams/" + attemptId + "/results");
        assertTrue(results.contains("100%"));
        assertTrue(results.contains("Correct answers"));
        assertTrue(results.contains("Short answers are not scored automatically"));
    }

    @Test
    void resubmittingAnExamCannotChangeTheScore() throws Exception {
        MockHttpSession session = registerAndSignIn(uniqueEmail());
        UUID documentId = createReadyDocument(session);
        UUID studyPackId = createStudyPackage(session, documentId);
        awaitStudyPackStatus(studyPackId, StudyPackageStatus.READY);

        MvcResult start = mockMvc.perform(post("/study/{id}/mock-exams", studyPackId)
                        .session(session).with(csrf()).param("durationMinutes", "20"))
                .andExpect(status().is3xxRedirection()).andReturn();
        UUID attemptId = UUID.fromString(start.getResponse().getRedirectedUrl()
                .substring(start.getResponse().getRedirectedUrl().lastIndexOf('/') + 1));

        mockMvc.perform(post("/exams/{id}/submit", attemptId).session(session).with(csrf()))
                .andExpect(status().is3xxRedirection());
        QuizAttempt first = quizAttemptRepository.findById(attemptId).orElseThrow();
        int scoreAfterFirstSubmit = first.getScorePercent();

        mockMvc.perform(post("/exams/{id}/submit", attemptId).session(session).with(csrf())
                        .param("anything", "anything"))
                .andExpect(status().is3xxRedirection());

        QuizAttempt second = quizAttemptRepository.findById(attemptId).orElseThrow();
        assertEquals(scoreAfterFirstSubmit, second.getScorePercent());
        assertEquals(first.getSubmittedAt(), second.getSubmittedAt());
    }

    @Test
    void tutorUsesRetrievedPassagesAndDropsInventedCitations() throws Exception {
        MockHttpSession session = registerAndSignIn(uniqueEmail());
        UUID documentId = createReadyDocument(session);
        UUID studyPackId = createStudyPackage(session, documentId);
        awaitStudyPackStatus(studyPackId, StudyPackageStatus.READY);

        mockMvc.perform(post("/study/{id}/tutor", studyPackId)
                        .session(session)
                        .with(csrf())
                        .param("question", "What is the difference between diffusion and osmosis?"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/study/" + studyPackId + "/tutor"));

        List<TutorMessage> messages = tutorMessageRepository
                .findByStudyPackageIdOrderByCreatedAtAscIdAsc(studyPackId);
        assertEquals(2, messages.size());
        assertEquals(MessageRole.USER, messages.get(0).getRole());
        assertEquals(MessageRole.ASSISTANT, messages.get(1).getRole());
        assertEquals(List.of("Page 1"), messages.get(1).getSourceReferences());
        assertFalse(messages.get(1).getSourceReferences().contains("Page 999"));

        String tutorPage = page(session, "/study/" + studyPackId + "/tutor");
        assertTrue(tutorPage.contains("Page 1"));
        assertFalse(tutorPage.contains("Page 999"));
    }

    @Test
    void studyPackagesAreIsolatedBetweenStudents() throws Exception {
        MockHttpSession alice = registerAndSignIn(uniqueEmail());
        UUID documentId = createReadyDocument(alice);
        UUID studyPackId = createStudyPackage(alice, documentId);
        awaitStudyPackStatus(studyPackId, StudyPackageStatus.READY);

        MockHttpSession bob = registerAndSignIn(uniqueEmail());
        mockMvc.perform(get("/study/{id}", studyPackId).session(bob))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/study/{id}/notes", studyPackId).session(bob))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/study/{id}/status", studyPackId).session(bob))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/study/{id}/mock-exams", studyPackId)
                        .session(bob).with(csrf()).param("durationMinutes", "20"))
                .andExpect(status().isNotFound());
    }

    @Test
    void invalidGeminiOutputFailsThePackageWithoutInventingContent() throws Exception {
        when(geminiClient.generate(any(GeminiRequest.class))).thenReturn(AiFixtures.invalidJson());

        MockHttpSession session = registerAndSignIn(uniqueEmail());
        UUID documentId = createReadyDocument(session);
        UUID studyPackId = createStudyPackage(session, documentId);

        StudyPackage failed = awaitStudyPackStatus(studyPackId, StudyPackageStatus.FAILED);
        assertNotNull(failed.getFailureReason());
        assertTrue(failed.getFailureReason().contains("Gemini"), failed.getFailureReason());
        assertTrue(tutorMessageRepository.findByStudyPackageIdOrderByCreatedAtAscIdAsc(studyPackId).isEmpty());

        String page = page(session, "/study/" + studyPackId);
        assertTrue(page.contains("Generation did not complete"));
        assertEquals(0, quizQuestionRepository
                .countByStudyPackageIdAndSection(studyPackId, QuestionSection.MOCK));
    }

    @Test
    void failedDocumentCannotStartGeneration() throws Exception {
        MockHttpSession session = registerAndSignIn(uniqueEmail());
        UUID documentId = uploadDocument(session, "scanned.pdf", "application/pdf",
                com.examprep.support.TestDocuments.pdf(""));
        awaitDocumentStatus(documentId, com.examprep.document.domain.DocumentStatus.FAILED);

        mockMvc.perform(post("/documents/{id}/study-packages", documentId)
                        .session(session).with(csrf()).param("language", "English"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/documents"));

        assertTrue(studyPackageRepository.findAll().stream()
                .noneMatch(pack -> pack.getDocumentId().equals(documentId)));
    }

    @Test
    void tutorWithoutConfiguredKeyWouldFailClearlyRatherThanFakeAnAnswer() throws Exception {
        // The mocked gateway is replaced by a fresh bean here to assert the real
        // configuration behaviour: no key means a clear configuration error.
        com.examprep.ai.config.AiProperties properties = new com.examprep.ai.config.AiProperties();
        properties.setApiKey("");
        com.examprep.ai.client.GoogleGeminiClient client =
                new com.examprep.ai.client.GoogleGeminiClient(properties);
        var exception = org.junit.jupiter.api.Assertions.assertThrows(
                com.examprep.ai.exception.AiConfigurationException.class,
                () -> client.generate(GeminiRequest.json("system", "prompt", 0.2)));
        assertTrue(exception.getMessage().contains("GOOGLE_API_KEY"));
    }

    @Test
    void generatedPackageSurvivesAndIsListedOnTheDashboard() throws Exception {
        MockHttpSession session = registerAndSignIn(uniqueEmail());
        UUID documentId = createReadyDocument(session);
        UUID studyPackId = createStudyPackage(session, documentId);
        awaitStudyPackStatus(studyPackId, StudyPackageStatus.READY);

        String dashboard = page(session, "/dashboard");
        assertTrue(dashboard.contains("Cell Membrane Transport"));
        assertTrue(dashboard.contains("Recent study packages"));

        String documents = page(session, "/documents");
        assertFalse(documents.contains("Nothing uploaded yet"));
        assertTrue(documents.contains("biology-notes.pdf"));

        assertTrue(mockMvc.perform(get("/study/{id}/notes", studyPackId).session(session))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8)
                .contains("Diffusion and osmosis"));
    }
}
