package com.examprep.document.parser;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.text.PDFTextStripper;

/**
 * Page-aware PDF extraction. Page numbers are real PDF page indexes, never invented.
 */
@org.springframework.stereotype.Component
public class PdfDocumentExtractor {

    public ExtractionResult extract(Path file) {
        try (PDDocument document = Loader.loadPDF(file.toFile())) {
            int pageCount = document.getNumberOfPages();
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            stripper.setLineSeparator("\n");

            List<ExtractedSection> sections = new ArrayList<>();
            for (int page = 1; page <= pageCount; page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                String text = stripper.getText(document);
                sections.add(new ExtractedSection("Page " + page, text));
            }
            return ExtractedSection.result(sections, pageCount);
        } catch (InvalidPasswordException ex) {
            throw new ExtractionException(
                    "This PDF is password protected. ExamPrep cannot read encrypted documents.", ex);
        } catch (Exception ex) {
            throw new ExtractionException(
                    "This PDF could not be read. It may be corrupted or use an unsupported encoding.", ex);
        }
    }
}
