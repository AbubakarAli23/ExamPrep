package com.examprep.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.examprep.document.parser.ExtractedSection;
import com.examprep.document.parser.ExtractionResult;
import com.examprep.document.service.DocumentChunker;
import java.util.List;
import org.junit.jupiter.api.Test;

class DocumentChunkerTest {

    private final DocumentChunker chunker = new DocumentChunker();

    @Test
    void keepsPageReferencesAndMarksSplitPages() {
        StringBuilder longPage = new StringBuilder();
        for (int index = 0; index < 60; index++) {
            longPage.append("Paragraph ").append(index)
                    .append(" explains a concept with enough detail to fill the page. ")
                    .append("It continues with supporting evidence and examples.\n\n");
        }
        ExtractionResult extraction = new ExtractionResult(List.of(
                new ExtractedSection("Page 1", "Short page about membranes and transport."),
                new ExtractedSection("Page 2", longPage.toString()),
                new ExtractedSection("Page 3", "Final page.")), 3, longPage.length() + 70);

        List<DocumentChunker.Chunk> chunks = chunker.chunk(extraction);

        assertTrue(chunks.stream().anyMatch(chunk -> chunk.reference().equals("Page 1")));
        assertTrue(chunks.stream().anyMatch(chunk -> chunk.reference().equals("Page 3")));
        assertTrue(chunks.stream().anyMatch(chunk -> chunk.reference().equals("Page 2 (part 1)")));
        assertTrue(chunks.stream().anyMatch(chunk -> chunk.reference().equals("Page 2 (part 2)")));
        assertTrue(chunks.stream().allMatch(chunk -> chunk.text().length() <= 3800));
    }

    @Test
    void ignoresEmptySectionsAndNormalisesWhitespace() {
        ExtractionResult extraction = new ExtractionResult(List.of(
                new ExtractedSection("Page 1", "   "),
                new ExtractedSection("Page 2", "Real content\r\n\r\nwith   spacing.")), 2, 30);

        List<DocumentChunker.Chunk> chunks = chunker.chunk(extraction);

        assertEquals(1, chunks.size());
        assertEquals("Page 2", chunks.get(0).reference());
        assertFalse(chunks.get(0).text().contains("\r"));
        assertFalse(chunks.get(0).text().contains("   "));
    }

    @Test
    void hardSplitsASingleOversizedParagraph() {
        String huge = "Sentence about the topic. ".repeat(600);
        ExtractionResult extraction = new ExtractionResult(
                List.of(new ExtractedSection("Slide 4", huge)), 1, huge.length());

        List<DocumentChunker.Chunk> chunks = chunker.chunk(extraction);

        assertTrue(chunks.size() > 1);
        assertTrue(chunks.stream().allMatch(chunk -> chunk.reference().startsWith("Slide 4")));
        assertTrue(chunks.stream().allMatch(chunk -> chunk.text().length() <= 3800));
    }
}
