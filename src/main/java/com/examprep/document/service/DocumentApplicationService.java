package com.examprep.document.service;

import com.examprep.document.dto.DocumentDtos.DocumentContext;
import com.examprep.document.dto.DocumentDtos.DocumentStatusDto;
import com.examprep.document.dto.DocumentDtos.DocumentSummaryDto;
import com.examprep.document.dto.DocumentDtos.DocumentUploadResult;
import java.util.List;
import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;

/**
 * Document module contract used by the web and AI modules.
 */
public interface DocumentApplicationService {

    DocumentUploadResult upload(MultipartFile file, UUID ownerId);

    DocumentStatusDto getStatus(UUID documentId, UUID ownerId);

    List<DocumentSummaryDto> listDocuments(UUID ownerId);

    DocumentContext getContext(UUID documentId, UUID ownerId);

    void delete(UUID documentId, UUID ownerId);
}
