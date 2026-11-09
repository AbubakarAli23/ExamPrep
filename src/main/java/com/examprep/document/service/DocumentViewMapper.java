package com.examprep.document.service;

import com.examprep.config.AppProperties;
import com.examprep.document.repository.DocumentChunkRepository;
import com.examprep.document.repository.DocumentRepository;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.springframework.stereotype.Component;
import com.examprep.document.domain.Document;
import com.examprep.document.dto.DocumentDtos.DocumentStatusDto;
import com.examprep.document.dto.DocumentDtos.DocumentSummaryDto;

/**
 * Maps document entities to the DTOs used by the browser.
 */
@Component
public class DocumentViewMapper {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", Locale.ENGLISH).withZone(ZoneId.systemDefault());

    private final AppProperties appProperties;

    public DocumentViewMapper(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    public DocumentStatusDto toStatus(Document document, long chunkCount) {
        return new DocumentStatusDto(
                document.getId(),
                document.getOriginalFilename(),
                document.getStatus().name(),
                progress(document),
                message(document),
                document.getStatus() == com.examprep.document.domain.DocumentStatus.FAILED
                        ? document.getFailureReason()
                        : null,
                document.getStatus() == com.examprep.document.domain.DocumentStatus.READY,
                document.getPageCount(),
                chunkCount,
                document.getCharacterCount());
    }

    public DocumentSummaryDto toSummary(Document document, long chunkCount) {
        return new DocumentSummaryDto(
                document.getId(),
                document.getOriginalFilename(),
                document.getContentType(),
                document.getStatus().name(),
                document.getFileSizeBytes(),
                sizeLabel(document.getFileSizeBytes()),
                document.getPageCount(),
                chunkCount,
                document.getFailureReason(),
                document.getCreatedAt() == null ? "" : DATE_FORMAT.format(document.getCreatedAt()),
                document.getCreatedAt());
    }

    private int progress(Document document) {
        return switch (document.getStatus()) {
            case UPLOADED -> 10;
            case PROCESSING -> 55;
            case READY -> 100;
            case FAILED -> 100;
        };
    }

    private String message(Document document) {
        return switch (document.getStatus()) {
            case UPLOADED -> "Queued for text extraction.";
            case PROCESSING -> "Reading the document and preparing study material.";
            case READY -> "Text extracted. You can generate a study package.";
            case FAILED -> document.getFailureReason() == null
                    ? "This document could not be processed."
                    : document.getFailureReason();
        };
    }

    public static String sizeLabel(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        if (bytes < 1024 * 1024) {
            return String.format(Locale.ENGLISH, "%.0f KB", bytes / 1024.0);
        }
        return String.format(Locale.ENGLISH, "%.1f MB", bytes / (1024.0 * 1024.0));
    }
}
