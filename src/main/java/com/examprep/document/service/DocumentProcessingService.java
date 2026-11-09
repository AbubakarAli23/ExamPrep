package com.examprep.document.service;

import com.examprep.config.AppProperties;
import com.examprep.document.domain.Document;
import com.examprep.document.domain.DocumentChunk;
import com.examprep.document.parser.ExtractionResult;
import com.examprep.document.parser.ExtractionException;
import com.examprep.document.parser.DocumentExtractionService;
import com.examprep.document.repository.DocumentChunkRepository;
import com.examprep.document.storage.FileStorage;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Asynchronous extraction pipeline: read the stored file, extract structured text, chunk
 * it, then persist chunks and flip the document to READY.
 */
@Service
public class DocumentProcessingService {

    private static final Logger log = LoggerFactory.getLogger(DocumentProcessingService.class);

    private final DocumentChunkRepository chunkRepository;
    private final DocumentStateService stateService;
    private final FileStorage fileStorage;
    private final DocumentExtractionService extractionService;
    private final DocumentChunker chunker;
    private final AppProperties appProperties;

    public DocumentProcessingService(DocumentChunkRepository chunkRepository,
                                     DocumentStateService stateService,
                                     FileStorage fileStorage,
                                     DocumentExtractionService extractionService,
                                     DocumentChunker chunker,
                                     AppProperties appProperties) {
        this.chunkRepository = chunkRepository;
        this.stateService = stateService;
        this.fileStorage = fileStorage;
        this.extractionService = extractionService;
        this.chunker = chunker;
        this.appProperties = appProperties;
    }

    @Async("documentTaskExecutor")
    public void process(UUID documentId) {
        Document document = stateService.startProcessing(documentId);
        if (document == null) {
            log.warn("Document {} disappeared before processing started", documentId);
            return;
        }

        Path temporaryFile = null;
        try {
            if (!fileStorage.exists(document.getStorageKey())) {
                throw new ExtractionException("The stored document could not be found. Please upload it again.");
            }
            temporaryFile = Files.createTempFile("examprep-", ".upload");
            try (InputStream input = fileStorage.open(document.getStorageKey())) {
                Files.copy(input, temporaryFile, StandardCopyOption.REPLACE_EXISTING);
            }

            ExtractionResult extraction = extractionService.extract(
                    temporaryFile, document.getOriginalFilename(), document.getContentType());

            List<DocumentChunker.Chunk> chunks = chunker.chunk(extraction);
            if (chunks.isEmpty() || extraction.characterCount() < 40) {
                throw new ExtractionException(noTextMessage(extraction));
            }
            storeChunks(documentId, chunks);
            stateService.markReady(documentId, extraction.pageCount(), extraction.characterCount());
            log.info("Document {} extracted into {} chunks ({} characters)",
                    documentId, chunks.size(), extraction.characterCount());
        } catch (ExtractionException ex) {
            log.info("Document {} could not be extracted: {}", documentId, ex.getMessage());
            stateService.markFailed(documentId, ex.getMessage());
        } catch (Exception ex) {
            log.error("Unexpected failure while extracting document {}", documentId, ex);
            stateService.markFailed(documentId,
                    "This document could not be processed. Please check the file and try again.");
        } finally {
            if (temporaryFile != null) {
                try {
                    Files.deleteIfExists(temporaryFile);
                } catch (Exception ignored) {
                    // Temporary file cleanup is best effort.
                }
            }
        }
    }

    private String noTextMessage(ExtractionResult extraction) {
        boolean pdf = extraction.pageCount() > 0;
        if (pdf) {
            return "No selectable text was found in this PDF. It looks like a scanned document, "
                    + "and ExamPrep does not perform OCR yet. Upload a text-based PDF or another format.";
        }
        return "No readable text was found in this document. It may contain only images or be empty.";
    }

    private void storeChunks(UUID documentId, List<DocumentChunker.Chunk> chunks) {
        List<DocumentChunk> entities = new ArrayList<>();
        for (int index = 0; index < chunks.size(); index++) {
            DocumentChunker.Chunk chunk = chunks.get(index);
            entities.add(new DocumentChunk(
                    UUID.randomUUID(), documentId, index, chunk.text(), chunk.reference()));
        }
        chunkRepository.saveAll(entities);
    }
}
