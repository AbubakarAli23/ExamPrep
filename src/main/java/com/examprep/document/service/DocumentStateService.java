package com.examprep.document.service;

import com.examprep.document.domain.Document;
import com.examprep.document.domain.DocumentStatus;
import com.examprep.document.repository.DocumentRepository;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Small transactional state transitions for document processing. Kept separate from the
 * long-running extraction so that status changes are visible to the browser immediately.
 */
@Service
public class DocumentStateService {

    private static final Logger log = LoggerFactory.getLogger(DocumentStateService.class);

    private final DocumentRepository documentRepository;

    public DocumentStateService(DocumentRepository documentRepository) {
        this.documentRepository = documentRepository;
    }

    @Transactional
    public Document startProcessing(UUID documentId) {
        Document document = documentRepository.findById(documentId).orElse(null);
        if (document == null) {
            return null;
        }
        document.setStatus(DocumentStatus.PROCESSING);
        document.setProcessingStartedAt(Instant.now());
        document.setFailureReason(null);
        return documentRepository.save(document);
    }

    @Transactional
    public void markFailed(UUID documentId, String reason) {
        documentRepository.findById(documentId).ifPresent(document -> {
            document.setStatus(DocumentStatus.FAILED);
            document.setFailureReason(truncate(reason));
            documentRepository.save(document);
            log.info("Document {} failed: {}", documentId, reason);
        });
    }

    @Transactional
    public void markReady(UUID documentId, int pageCount, int characterCount) {
        documentRepository.findById(documentId).ifPresent(document -> {
            document.setStatus(DocumentStatus.READY);
            document.setPageCount(pageCount > 0 ? pageCount : null);
            document.setCharacterCount(characterCount);
            document.setFailureReason(null);
            documentRepository.save(document);
        });
    }

    @Transactional
    public List<Document> findStale(Instant cutoff) {
        return documentRepository.findByStatusInAndUpdatedAtBefore(
                EnumSet.of(DocumentStatus.UPLOADED, DocumentStatus.PROCESSING), cutoff);
    }

    @Transactional
    public void markInterrupted(UUID documentId) {
        documentRepository.findById(documentId).ifPresent(document -> {
            document.setStatus(DocumentStatus.FAILED);
            document.setFailureReason(
                    "Processing was interrupted by an application restart. Please delete the document and upload it again.");
            documentRepository.save(document);
        });
    }

    private String truncate(String reason) {
        if (reason == null) {
            return "Processing failed.";
        }
        return reason.length() > 950 ? reason.substring(0, 950) : reason;
    }
}
