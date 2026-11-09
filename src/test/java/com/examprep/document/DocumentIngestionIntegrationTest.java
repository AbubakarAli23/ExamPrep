package com.examprep.document;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.examprep.document.domain.Document;
import com.examprep.document.domain.DocumentStatus;
import com.examprep.document.dto.DocumentDtos.DocumentContext;
import com.examprep.document.service.DocumentApplicationService;
import com.examprep.support.AbstractIntegrationTest;
import com.examprep.support.TestDocuments;
import com.examprep.user.repository.UserAccountRepository;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;

class DocumentIngestionIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private DocumentApplicationService documentApplicationService;

    @Autowired
    private UserAccountRepository userAccountRepository;

    private UUID ownerId(String email) {
        return userAccountRepository.findByEmail(email).orElseThrow().getId();
    }

    @Test
    void pdfExtractionKeepsRealPageReferences() throws Exception {
        String email = uniqueEmail();
        MockHttpSession session = registerAndSignIn(email);

        byte[] pdf = TestDocuments.pdf(
                "The cell membrane is selectively permeable and controls transport.",
                "Active transport uses ATP and carrier proteins.");
        UUID documentId = uploadDocument(session, "membrane.pdf", "application/pdf", pdf);
        awaitDocumentStatus(documentId, DocumentStatus.READY);

        DocumentContext context = documentApplicationService.getContext(documentId, ownerId(email));
        List<String> references = context.chunks().stream().map(chunk -> chunk.reference()).toList();
        assertTrue(references.contains("Page 1"), "Expected a real Page 1 reference, got " + references);
        assertTrue(references.contains("Page 2"), "Expected a real Page 2 reference, got " + references);
        assertTrue(context.chunks().stream().anyMatch(chunk -> chunk.text().contains("selectively permeable")));
    }

    @Test
    void docxExtractionProducesChunks() throws Exception {
        String email = uniqueEmail();
        MockHttpSession session = registerAndSignIn(email);
        byte[] docx = TestDocuments.docx(
                "Photosynthesis converts light energy into chemical energy.",
                "Chlorophyll absorbs light in the visible spectrum.");
        UUID documentId = uploadDocument(session, "photosynthesis.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document", docx);
        awaitDocumentStatus(documentId, DocumentStatus.READY);

        DocumentContext context = documentApplicationService.getContext(documentId, ownerId(email));
        assertFalse(context.chunks().isEmpty());
        assertTrue(context.chunks().stream().anyMatch(chunk -> chunk.text().contains("Photosynthesis")));
        assertEquals("Document", context.chunks().get(0).reference());
    }

    @Test
    void pptxExtractionKeepsSlideReferences() throws Exception {
        String email = uniqueEmail();
        MockHttpSession session = registerAndSignIn(email);
        byte[] pptx = TestDocuments.pptx(
                "Lecture 1: Introduction to statistics and data types",
                "Lecture 2: Measures of central tendency and spread");
        UUID documentId = uploadDocument(session, "statistics.pptx",
                "application/vnd.openxmlformats-officedocument.presentationml.presentation", pptx);
        awaitDocumentStatus(documentId, DocumentStatus.READY);

        DocumentContext context = documentApplicationService.getContext(documentId, ownerId(email));
        List<String> references = context.chunks().stream().map(chunk -> chunk.reference()).toList();
        assertTrue(references.contains("Slide 1"), "Expected Slide 1, got " + references);
        assertTrue(references.contains("Slide 2"), "Expected Slide 2, got " + references);
    }

    @Test
    void plainTextMarkdownBecomesHeadedSections() throws Exception {
        String email = uniqueEmail();
        MockHttpSession session = registerAndSignIn(email);
        String markdown = "# Regression\n\nLinear regression fits a line to data.\n\n## Assumptions\n\nErrors are independent.";
        UUID documentId = uploadDocument(session, "notes.md", "text/markdown",
                markdown.getBytes(StandardCharsets.UTF_8));
        awaitDocumentStatus(documentId, DocumentStatus.READY);

        DocumentContext context = documentApplicationService.getContext(documentId, ownerId(email));
        assertTrue(context.chunks().stream()
                .anyMatch(chunk -> chunk.reference().startsWith("Section: Regression")));
        assertTrue(context.chunks().stream()
                .anyMatch(chunk -> chunk.reference().startsWith("Section: Assumptions")));
    }

    @Test
    void unsupportedFileIsRejectedAtUpload() throws Exception {
        MockHttpSession session = registerAndSignIn(uniqueEmail());
        mockMvc.perform(multipart("/documents/upload-json")
                        .file(new MockMultipartFile("file", "payload.sh", "text/x-shellscript",
                                "#!/bin/sh\necho hi".getBytes(StandardCharsets.UTF_8)))
                        .session(session)
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(containsString("ExamPrep supports")));
    }

    @Test
    void oversizedFileIsRejected() throws Exception {
        MockHttpSession session = registerAndSignIn(uniqueEmail());
        // The test profile sets a 1 MB limit.
        byte[] big = new byte[1500 * 1024];
        java.util.Arrays.fill(big, (byte) 'a');
        mockMvc.perform(multipart("/documents/upload-json")
                        .file(new MockMultipartFile("file", "huge.txt", "text/plain", big))
                        .session(session)
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(containsString("limit")));
    }

    @Test
    void corruptedPdfFailsWithAReadableMessage() throws Exception {
        MockHttpSession session = registerAndSignIn(uniqueEmail());
        UUID documentId = uploadDocument(session, "broken.pdf", "application/pdf", TestDocuments.malformedPdf());
        Document document = awaitDocumentStatus(documentId, DocumentStatus.FAILED);
        assertNotNull(document.getFailureReason());
        assertFalse(document.getFailureReason().isBlank());
        assertTrue(document.getFailureReason().toLowerCase().contains("pdf")
                        || document.getFailureReason().toLowerCase().contains("corrupt")
                        || document.getFailureReason().toLowerCase().contains("read"),
                "Unexpected failure message: " + document.getFailureReason());
    }

    @Test
    void scannedPdfIsReportedAsHavingNoText() throws Exception {
        MockHttpSession session = registerAndSignIn(uniqueEmail());
        UUID documentId = uploadDocument(session, "scanned.pdf", "application/pdf", TestDocuments.pdf(""));
        Document document = awaitDocumentStatus(documentId, DocumentStatus.FAILED);
        assertTrue(document.getFailureReason().toLowerCase().contains("scanned"),
                "Expected a scanned-document explanation, got: " + document.getFailureReason());
    }

    @Test
    void studentsCannotReadAnotherStudentsDocument() throws Exception {
        MockHttpSession alice = registerAndSignIn(uniqueEmail());
        UUID documentId = createReadyDocument(alice);

        MockHttpSession bob = registerAndSignIn(uniqueEmail());
        mockMvc.perform(get("/documents/{id}/status", documentId).session(bob))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/documents/{id}/study-packages", documentId)
                        .session(bob)
                        .with(csrf())
                        .param("language", "English"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/documents"));
    }

    @Test
    void deletingADocumentRemovesItFromStorage() throws Exception {
        MockHttpSession session = registerAndSignIn(uniqueEmail());
        UUID documentId = createReadyDocument(session);

        mockMvc.perform(post("/documents/{id}/delete", documentId).session(session).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/documents"));

        assertFalse(documentRepository.findById(documentId).isPresent());
    }
}
