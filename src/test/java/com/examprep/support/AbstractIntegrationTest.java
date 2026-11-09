package com.examprep.support;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.examprep.document.domain.Document;
import com.examprep.document.domain.DocumentStatus;
import com.examprep.document.repository.DocumentRepository;
import com.examprep.study.domain.StudyPackage;
import com.examprep.study.domain.StudyPackageStatus;
import com.examprep.study.repository.StudyPackageRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Base for browser-level integration tests. Uses a real PostgreSQL server, real Flyway
 * migrations and real document parsers; only the Gemini gateway is mocked by subclasses
 * that need it.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestAsyncConfig.class)
public abstract class AbstractIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected DocumentRepository documentRepository;

    @Autowired
    protected StudyPackageRepository studyPackageRepository;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",
                () -> PostgresSupport.get().getJdbcUrl("postgres", "postgres"));
        registry.add("spring.datasource.username", () -> "postgres");
        registry.add("spring.datasource.password", () -> "");
        registry.add("app.storage.location", StorageSupport::location);
        registry.add("app.max-upload-size-mb", () -> 1);
        registry.add("app.ai.api-key", () -> "test-key-never-used-for-real-calls");
        registry.add("app.ai.model", () -> "gemini-3.8-flash");
    }

    protected String uniqueEmail() {
        return "student-" + UUID.randomUUID() + "@example.test";
    }

    protected void register(String email) throws Exception {
        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("fullName", "Test Student")
                        .param("email", email)
                        .param("password", "Password123"))
                .andExpect(status().is3xxRedirection());
    }

    protected MockHttpSession registerAndSignIn(String email) throws Exception {
        register(email);
        return signIn(email);
    }

    protected MockHttpSession signIn(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("email", email)
                        .param("password", "Password123"))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
        assertNotNull(session, "Sign in should create a session");
        return session;
    }

    protected UUID uploadDocument(MockHttpSession session, String filename, String contentType, byte[] content)
            throws Exception {
        MvcResult result = mockMvc.perform(multipart("/documents/upload-json")
                        .file(new MockMultipartFile("file", filename, contentType, content))
                        .session(session)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andReturn();
        String body = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        return UUID.fromString(objectMapper.readTree(body).get("documentId").asText());
    }

    protected Document awaitDocumentStatus(UUID documentId, DocumentStatus expected) {
        await().atMost(Duration.ofSeconds(30)).pollInterval(Duration.ofMillis(200)).until(() ->
                documentRepository.findById(documentId)
                        .map(document -> document.getStatus() == expected)
                        .orElse(false));
        return documentRepository.findById(documentId).orElseThrow();
    }

    protected StudyPackage awaitStudyPackStatus(UUID studyPackId, StudyPackageStatus expected) {
        await().atMost(Duration.ofSeconds(30)).pollInterval(Duration.ofMillis(200)).until(() ->
                studyPackageRepository.findById(studyPackId)
                        .map(studyPack -> studyPack.getStatus() == expected)
                        .orElse(false));
        return studyPackageRepository.findById(studyPackId).orElseThrow();
    }

    protected UUID createReadyDocument(MockHttpSession session) throws Exception {
        byte[] pdf = TestDocuments.pdf(
                "Cell membranes control what enters and leaves the cell. Diffusion and osmosis are passive processes.",
                "Active transport requires ATP and membrane proteins to move substances against a gradient.");
        UUID documentId = uploadDocument(session, "biology-notes.pdf", "application/pdf", pdf);
        awaitDocumentStatus(documentId, DocumentStatus.READY);
        return documentId;
    }

    protected UUID createStudyPackage(MockHttpSession session, UUID documentId) throws Exception {
        MvcResult result = mockMvc.perform(post("/documents/{id}/study-packages", documentId)
                        .session(session)
                        .with(csrf())
                        .param("language", "English")
                        .param("academicLevel", "Undergraduate")
                        .param("practiceQuestionCount", "8")
                        .param("mockQuestionCount", "6")
                        .param("flashcardCount", "8"))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        String location = result.getResponse().getRedirectedUrl();
        assertNotNull(location);
        String id = location.substring(location.lastIndexOf('/') + 1);
        return UUID.fromString(id);
    }

    protected String page(MockHttpSession session, String url) throws Exception {
        return mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(url)
                        .session(session))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
    }
}
