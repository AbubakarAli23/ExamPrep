package com.examprep.document.parser;

import java.nio.file.Path;
import org.springframework.stereotype.Component;

/**
 * Routes a stored file to the extractor that understands its structure.
 */
@Component
public class DocumentExtractionService {

    private final PdfDocumentExtractor pdfDocumentExtractor;
    private final PptxDocumentExtractor pptxDocumentExtractor;
    private final TikaDocumentExtractor tikaDocumentExtractor;

    public DocumentExtractionService(PdfDocumentExtractor pdfDocumentExtractor,
                                     PptxDocumentExtractor pptxDocumentExtractor,
                                     TikaDocumentExtractor tikaDocumentExtractor) {
        this.pdfDocumentExtractor = pdfDocumentExtractor;
        this.pptxDocumentExtractor = pptxDocumentExtractor;
        this.tikaDocumentExtractor = tikaDocumentExtractor;
    }

    public ExtractionResult extract(Path file, String filename, String contentType) {
        String type = contentType == null ? "" : contentType.toLowerCase();
        if (type.equals("application/pdf")) {
            return pdfDocumentExtractor.extract(file);
        }
        if (type.contains("presentationml.presentation")) {
            return pptxDocumentExtractor.extract(file);
        }
        return tikaDocumentExtractor.extract(file, filename);
    }
}
