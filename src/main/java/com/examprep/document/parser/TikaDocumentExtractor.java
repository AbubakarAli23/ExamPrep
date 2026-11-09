package com.examprep.document.parser;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.stereotype.Component;

/**
 * General extraction for DOCX, DOC, TXT, Markdown, RTF, ODT and ODP. Markdown heading
 * markers become section references; other formats fall back to a general document
 * reference instead of invented page numbers.
 */
@Component
public class TikaDocumentExtractor {

    private static final int MAX_CHARACTERS = 3_000_000;

    public ExtractionResult extract(Path file, String filename) {
        try (InputStream input = Files.newInputStream(file)) {
            AutoDetectParser parser = new AutoDetectParser();
            BodyContentHandler handler = new BodyContentHandler(MAX_CHARACTERS);
            Metadata metadata = new Metadata();
            metadata.set(TikaCoreProperties.RESOURCE_NAME_KEY, filename);
            parser.parse(input, handler, metadata, new ParseContext());
            String text = handler.toString();
            List<ExtractedSection> sections = splitSections(text, filename);
            return ExtractedSection.result(sections, 0);
        } catch (Exception ex) {
            throw new ExtractionException(
                    "This document could not be read. It may be corrupted or protected by a password.", ex);
        }
    }

    private List<ExtractedSection> splitSections(String text, String filename) {
        List<ExtractedSection> sections = new ArrayList<>();
        boolean markdown = filename != null && filename.toLowerCase().endsWith(".md");
        if (!markdown) {
            sections.add(new ExtractedSection("Document", text));
            return sections;
        }

        String heading = null;
        StringBuilder body = new StringBuilder();
        for (String line : text.split("\\R", -1)) {
            String trimmed = line.strip();
            if (trimmed.startsWith("#")) {
                int level = 0;
                while (level < trimmed.length() && trimmed.charAt(level) == '#') {
                    level++;
                }
                String title = trimmed.substring(level).strip();
                if (level <= 3 && !title.isEmpty()) {
                    if (!body.isEmpty()) {
                        sections.add(new ExtractedSection(
                                heading == null ? "Document" : heading, body.toString()));
                        body.setLength(0);
                    }
                    heading = "Section: " + title;
                    continue;
                }
            }
            body.append(line).append('\n');
        }
        if (!body.isEmpty()) {
            sections.add(new ExtractedSection(heading == null ? "Document" : heading, body.toString()));
        }
        if (sections.isEmpty()) {
            sections.add(new ExtractedSection("Document", text));
        }
        return sections;
    }
}
