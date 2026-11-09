package com.examprep.document.service;

import com.examprep.config.AppProperties;
import com.examprep.document.service.InvalidUploadException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Set;
import org.apache.tika.Tika;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * Validates uploads without trusting the browser: the extension must be supported and the
 * bytes themselves must be detected as a supported document type.
 */
@Component
public class DocumentUploadValidator {

    public static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "pdf", "docx", "doc", "pptx", "ppt", "txt", "md", "markdown", "rtf", "odt", "odp");

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-powerpoint",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "application/vnd.oasis.opendocument.text",
            "application/vnd.oasis.opendocument.presentation",
            "application/rtf",
            "text/rtf",
            "text/plain",
            "text/markdown",
            "text/x-markdown");

    private final AppProperties appProperties;
    private final Tika tika = new Tika();

    public DocumentUploadValidator(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    public ValidatedUpload validate(MultipartFile file) {
        if (file == null || file.isEmpty() || file.getSize() == 0) {
            throw new InvalidUploadException("Please choose a document to upload.");
        }

        long maxBytes = (long) appProperties.getMaxUploadSizeMb() * 1024L * 1024L;
        if (file.getSize() > maxBytes) {
            throw new InvalidUploadException(
                    "That document is larger than the " + appProperties.getMaxUploadSizeMb() + " MB limit.");
        }

        String originalFilename = sanitizeFilename(file.getOriginalFilename());
        String extension = extensionOf(originalFilename);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new InvalidUploadException(
                    "ExamPrep supports PDF, DOCX, DOC, PPTX, PPT, TXT, Markdown, RTF, ODT and ODP documents.");
        }

        String detectedType;
        try (InputStream input = file.getInputStream()) {
            detectedType = normalize(tika.detect(input, originalFilename));
        } catch (IOException ex) {
            throw new InvalidUploadException("The uploaded document could not be read.");
        }

        if (!ALLOWED_CONTENT_TYPES.contains(detectedType)) {
            throw new InvalidUploadException(
                    "The uploaded file does not appear to be a supported academic document. "
                            + "Its detected type was " + detectedType + ".");
        }

        return new ValidatedUpload(originalFilename, extension, detectedType, file.getSize());
    }

    private String sanitizeFilename(String filename) {
        String name = filename == null ? "" : filename.trim();
        // Strip any path components supplied by the client.
        name = name.replace('\\', '/');
        int slash = name.lastIndexOf('/');
        if (slash >= 0) {
            name = name.substring(slash + 1);
        }
        name = name.replaceAll("[\\p{Cntrl}]", "");
        if (name.isBlank()) {
            throw new InvalidUploadException("The uploaded file needs a filename.");
        }
        if (name.length() > 255) {
            name = name.substring(name.length() - 255);
        }
        return name;
    }

    private String extensionOf(String filename) {
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) {
            return "";
        }
        return filename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private String normalize(String contentType) {
        if (contentType == null) {
            return "";
        }
        int semicolon = contentType.indexOf(';');
        String base = semicolon >= 0 ? contentType.substring(0, semicolon) : contentType;
        return base.trim().toLowerCase(Locale.ROOT);
    }

    public record ValidatedUpload(
            String originalFilename,
            String extension,
            String detectedContentType,
            long sizeBytes) {
    }
}
