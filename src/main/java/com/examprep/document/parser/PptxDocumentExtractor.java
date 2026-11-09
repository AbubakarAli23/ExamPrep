package com.examprep.document.parser;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFGroupShape;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTable;
import org.apache.poi.xslf.usermodel.XSLFTableCell;
import org.apache.poi.xslf.usermodel.XSLFTableRow;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.springframework.stereotype.Component;

/**
 * Extracts PPTX decks slide by slide so citations can name real slides.
 */
@Component
public class PptxDocumentExtractor {

    public ExtractionResult extract(Path file) {
        try (InputStream input = Files.newInputStream(file);
             XMLSlideShow slideShow = new XMLSlideShow(input)) {

            List<XSLFSlide> slides = slideShow.getSlides();
            List<ExtractedSection> sections = new ArrayList<>();
            for (int index = 0; index < slides.size(); index++) {
                StringBuilder text = new StringBuilder();
                for (XSLFShape shape : slides.get(index).getShapes()) {
                    collectShapeText(shape, text);
                }
                sections.add(new ExtractedSection("Slide " + (index + 1), text.toString()));
            }
            return ExtractedSection.result(sections, slides.size());
        } catch (Exception ex) {
            throw new ExtractionException(
                    "This presentation could not be read. It may be corrupted or password protected.", ex);
        }
    }

    private void collectShapeText(XSLFShape shape, StringBuilder target) {
        if (shape instanceof XSLFTextShape textShape) {
            String text = textShape.getText();
            if (text != null && !text.isBlank()) {
                target.append(text.strip()).append('\n');
            }
        } else if (shape instanceof XSLFGroupShape group) {
            for (XSLFShape child : group.getShapes()) {
                collectShapeText(child, target);
            }
        } else if (shape instanceof XSLFTable table) {
            for (XSLFTableRow row : table.getRows()) {
                List<String> cells = new ArrayList<>();
                for (XSLFTableCell cell : row.getCells()) {
                    cells.add(cell.getText() == null ? "" : cell.getText().strip());
                }
                target.append(String.join(" | ", cells)).append('\n');
            }
        }
    }
}
