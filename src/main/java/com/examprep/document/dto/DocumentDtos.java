package com.examprep.document.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Shared document DTOs. Controllers and the AI module consume these records only; JPA
 * entities never leave the service layer.
 */
public final class DocumentDtos {

    private DocumentDtos() {
    }

    public record DocumentUploadResult(
            UUID documentId,
            String originalFilename,
            String status,
            String message) {
    }

    public record DocumentStatusDto(
            UUID documentId,
            String originalFilename,
            String status,
            int progressPercent,
            String message,
            String failureReason,
            boolean ready,
            Integer pageCount,
            long chunkCount,
            Integer characterCount) {
    }

    public record DocumentSummaryDto(
            UUID documentId,
            String originalFilename,
            String contentType,
            String status,
            long sizeBytes,
            String sizeLabel,
            Integer pageCount,
            long chunkCount,
            String failureReason,
            String createdAtLabel,
            Instant createdAt) {
    }

    public record DocumentChunkDto(
            UUID chunkId,
            String reference,
            String text) {
    }

    public record DocumentContext(
            UUID documentId,
            String title,
            List<DocumentChunkDto> chunks) {
    }
}
