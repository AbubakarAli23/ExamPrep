package com.examprep.support;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextBox;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;

/**
 * Builds genuine PDF, DOCX and PPTX byte arrays so extraction is tested against real
 * documents rather than mocked parsers.
 */
public final class TestDocuments {

    private TestDocuments() {
    }

    public static byte[] pdf(String... pageTexts) {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            for (String pageText : pageTexts) {
                PDPage page = new PDPage();
                document.addPage(page);
                if (pageText != null && !pageText.isBlank()) {
                    try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                        content.beginText();
                        content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                        content.newLineAtOffset(72, 720);
                        int line = 0;
                        for (String part : pageText.split("\n")) {
                            content.showText(part);
                            content.newLineAtOffset(0, -16);
                            line++;
                            if (line > 12) {
                                break;
                            }
                        }
                        content.endText();
                    }
                }
            }
            document.save(output);
            return output.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("Could not build test PDF", ex);
        }
    }

    public static byte[] docx(String... paragraphs) {
        try (XWPFDocument document = new XWPFDocument();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            for (String text : paragraphs) {
                XWPFParagraph paragraph = document.createParagraph();
                XWPFRun run = paragraph.createRun();
                run.setText(text);
            }
            document.write(output);
            return output.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("Could not build test DOCX", ex);
        }
    }

    public static byte[] pptx(String... slides) {
        try (XMLSlideShow slideShow = new XMLSlideShow();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            for (String text : slides) {
                XSLFSlide slide = slideShow.createSlide();
                XSLFTextBox box = slide.createTextBox();
                box.setAnchor(new java.awt.Rectangle(50, 50, 500, 300));
                box.setText(text);
            }
            slideShow.write(output);
            return output.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("Could not build test PPTX", ex);
        }
    }

    public static byte[] plainText(String text) {
        return text.getBytes(StandardCharsets.UTF_8);
    }

    public static byte[] malformedPdf() {
        return "%PDF-1.4\nthis file is not a real PDF and has no xref".getBytes(StandardCharsets.UTF_8);
    }
}
