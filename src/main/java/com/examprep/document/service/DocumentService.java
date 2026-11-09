package com.examprep.document.service;

import com.examprep.document.domain.Document;
import com.examprep.document.domain.DocumentStatus;
import com.examprep.document.dto.DocumentDtos.DocumentContext;
import com.examprep.document.dto.DocumentDtos.DocumentStatusDto;
import com.examprep.document.dto.DocumentDtos.DocumentSummaryDto;
import com.examprep.document.dto.DocumentDtos.DocumentUploadResult;
import com.examprep.document.dto.DocumentDtos.DocumentChunkDto;
import com.examprep.common.exception.ResourceNotFoundException;
import com.examprep.document.domain.DocumentChunk;
import com.examprep.document.repository.DocumentChunkRepository;
import com.examprep.document.repository.DocumentRepository;
import com.examprep.document.storage.FileStorage;
import com.examprep.document.storage.FileStorage.StoredFile;
import com.examprep.document.service.DocumentUploadValidator.ValidatedUpload;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class DocumentService implements DocumentApplicationService {

    private static final Logger log = LoggerFactory.getLogger(DocumentService.class);

    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository chunkRepository;
    private final DocumentUploadValidator uploadValidator;
    private final FileStorage fileStorage;
    private final ApplicationEventPublisher eventPublisher;
    private final DocumentViewMapper viewMapper;

    public DocumentService(DocumentRepository documentRepository,
                           DocumentChunkRepository chunkRepository,
                           DocumentUploadValidator uploadValidator,
                           FileStorage fileStorage,
                           ApplicationEventPublisher eventPublisher,
                           DocumentViewMapper viewMapper) {
        this.documentRepository = documentRepository;
        this.chunkRepository = chunkRepository;
        this.uploadValidator = uploadValidator;
        this.fileStorage = fileStorage;
        this.eventPublisher = eventPublisher;
        this.viewMapper = viewMapper;
    }

    @Override
    @Transactional
    public DocumentUploadResult upload(MultipartFile file, UUID ownerId) {
        ValidatedUpload validated = uploadValidator.validate(file);

        StoredFile stored;
        try {
            stored = fileStorage.store(file, validated.extension());
        } catch (Exception ex) {
            log.error("Failed to store an uploaded document for owner {}", ownerId, ex);
            throw new DocumentStorageException("The document could not be stored. Please try again.", ex);
        }

        Document document = new Document(
                UUID.randomUUID(),
                ownerId,
                validated.originalFilename(),
                validated.detectedContentType(),
                stored.sizeBytes(),
                stored.key(),
                DocumentStatus.UPLOADED);
        documentRepository.save(document);

        // Extraction starts only after the upload transaction has committed.
        eventPublisher.publishEvent(new DocumentUploadedEvent(document.getId(), ownerId));
        log.info("Stored document {} ({} bytes) for owner {}", document.getId(), stored.sizeBytes(), ownerId);

        return new DocumentUploadResult(
                document.getId(),
                document.getOriginalFilename(),
                document.getStatus().name(),
                "Upload received. ExamPrep is reading the document now.");
    }

    @Override
    @Transactional(readOnly = true)
    public DocumentStatusDto getStatus(UUID documentId, UUID ownerId) {
        Document document = requireOwnedDocument(documentId, ownerId);
        long chunkCount = chunkRepository.countByDocumentId(documentId);
        return viewMapper.toStatus(document, chunkCount);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentSummaryDto> listDocuments(UUID ownerId) {
        return documentRepository.findByOwnerIdOrderByCreatedAtDesc(ownerId).stream()
                .map(document -> viewMapper.toSummary(document, chunkRepository.countByDocumentId(document.getId())))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public DocumentContext getContext(UUID documentId, UUID ownerId) {
        Document document = requireOwnedDocument(documentId, ownerId);
        List<DocumentChunkDto> chunks = chunkRepository.findByDocumentIdOrderByChunkIndexAsc(documentId).stream()
                .map(this::toChunkDto)
                .toList();
        return new DocumentContext(document.getId(), document.getOriginalFilename(), chunks);
    }

    @Override
    @Transactional
    public void delete(UUID documentId, UUID ownerId) {
        Document document = requireOwnedDocument(documentId, ownerId);
        String storageKey = document.getStorageKey();
        documentRepository.delete(document);
        documentRepository.flush();
        if (storageKey != null) {
            fileStorage.delete(storageKey);
        }
        log.info("Deleted document {} for owner {}", documentId, ownerId);
    }

    private DocumentChunkDto toChunkDto(DocumentChunk chunk) {
        return new DocumentChunkDto(chunk.getId(), chunk.getSourceReference(), chunk.getText());
    }

    private Document requireOwnedDocument(UUID documentId, UUID ownerId) {
        return documentRepository
                .findByIdAndOwnerId(documentId, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("That document could not be found."));
    }
}
