package com.examprep.document.service;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Starts extraction once the upload transaction has committed, so the background worker
 * can always see the document row.
 */
@Component
public class DocumentUploadListener {

    private final DocumentProcessingService processingService;

    public DocumentUploadListener(DocumentProcessingService processingService) {
        this.processingService = processingService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDocumentUploaded(DocumentUploadedEvent event) {
        processingService.process(event.documentId());
    }
}
