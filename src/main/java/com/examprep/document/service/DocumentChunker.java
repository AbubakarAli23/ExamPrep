package com.examprep.document.service;

import com.examprep.document.parser.ExtractedSection;
import com.examprep.document.parser.ExtractionResult;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

/**
 * Splits extracted sections into retrieval-sized chunks while keeping a source reference
 * for every chunk. Page and slide references are preserved; only very long sections are
 * split, and the part number is made explicit.
 */
@Component
public class DocumentChunker {

    static final int MAX_CHUNK_CHARACTERS = 3600;
    private static final int MIN_CHUNK_CHARACTERS = 120;

    public List<Chunk> chunk(ExtractionResult extraction) {
        List<Chunk> chunks = new ArrayList<>();
        for (ExtractedSection section : extraction.sections()) {
            String cleaned = clean(section.text());
            if (cleaned.isBlank()) {
                continue;
            }
            List<String> pieces = split(cleaned);
            for (int index = 0; index < pieces.size(); index++) {
                String reference = section.reference();
                if (pieces.size() > 1) {
                    reference = reference + " (part " + (index + 1) + ")";
                }
                chunks.add(new Chunk(reference, pieces.get(index)));
            }
        }
        return chunks;
    }

    private List<String> split(String text) {
        List<String> pieces = new ArrayList<>();
        StringBuilder buffer = new StringBuilder();
        for (String paragraph : text.split("\\n\\s*\\n")) {
            String normalized = paragraph.strip();
            if (normalized.isEmpty()) {
                continue;
            }
            if (normalized.length() > MAX_CHUNK_CHARACTERS) {
                if (!buffer.isEmpty()) {
                    pieces.add(buffer.toString().strip());
                    buffer.setLength(0);
                }
                pieces.addAll(splitLongParagraph(normalized));
                continue;
            }
            if (buffer.length() + normalized.length() + 2 > MAX_CHUNK_CHARACTERS && buffer.length() > 0) {
                pieces.add(buffer.toString().strip());
                buffer.setLength(0);
            }
            buffer.append(normalized).append("\n\n");
        }
        if (!buffer.isEmpty()) {
            pieces.add(buffer.toString().strip());
        }

        // Merge a tiny trailing fragment into the previous chunk rather than keeping it alone.
        if (pieces.size() > 1) {
            int lastIndex = pieces.size() - 1;
            String last = pieces.get(lastIndex);
            if (last.length() < MIN_CHUNK_CHARACTERS) {
                String merged = pieces.get(lastIndex - 1) + "\n\n" + last;
                if (merged.length() <= MAX_CHUNK_CHARACTERS + MIN_CHUNK_CHARACTERS) {
                    pieces.set(lastIndex - 1, merged);
                    pieces.remove(lastIndex);
                }
            }
        }
        return pieces;
    }

    private List<String> splitLongParagraph(String paragraph) {
        List<String> pieces = new ArrayList<>();
        int start = 0;
        while (start < paragraph.length()) {
            int end = Math.min(start + MAX_CHUNK_CHARACTERS, paragraph.length());
            if (end < paragraph.length()) {
                int boundary = lastSentenceBoundary(paragraph, start, end);
                if (boundary > start) {
                    end = boundary;
                }
            }
            pieces.add(paragraph.substring(start, end).strip());
            start = end;
        }
        return pieces;
    }

    private int lastSentenceBoundary(String text, int start, int end) {
        for (int index = end; index > start + (MAX_CHUNK_CHARACTERS / 2); index--) {
            char character = text.charAt(index - 1);
            if (character == '.' || character == '!' || character == '?' || character == '\n') {
                return index;
            }
        }
        return end;
    }

    private String clean(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("\u0000", "")
                .replace("\r\n", "\n")
                .replace('\r', '\n')
                .replaceAll("[\\t\\x0B\\f]+", " ")
                .replaceAll(" {2,}", " ")
                .replaceAll("\\n{3,}", "\n\n")
                .strip();
    }

    public record Chunk(String reference, String text) {
    }
}
