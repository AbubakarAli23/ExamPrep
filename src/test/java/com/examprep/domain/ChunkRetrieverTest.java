package com.examprep.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.examprep.ai.retrieval.ChunkRetriever;
import com.examprep.document.dto.DocumentDtos.DocumentChunkDto;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ChunkRetrieverTest {

    private final ChunkRetriever retriever = new ChunkRetriever();

    @Test
    void ranksTheChunkThatActuallyDiscussesTheQuestion() {
        List<DocumentChunkDto> chunks = List.of(
                new DocumentChunkDto(UUID.randomUUID(), "Page 1",
                        "The cell membrane controls what enters and leaves the cell."),
                new DocumentChunkDto(UUID.randomUUID(), "Page 2",
                        "Osmosis is the movement of water across a selectively permeable membrane."),
                new DocumentChunkDto(UUID.randomUUID(), "Page 3",
                        "Mitochondria produce ATP through cellular respiration."));

        List<DocumentChunkDto> selected = retriever.retrieve(chunks, "Explain osmosis and water movement", 2, 5000);

        assertFalse(selected.isEmpty());
        assertEquals("Page 2", selected.get(0).reference());
        assertTrue(selected.size() <= 2);
    }

    @Test
    void fallsBackToTheOpeningMaterialWhenNothingMatches() {
        List<DocumentChunkDto> chunks = List.of(
                new DocumentChunkDto(UUID.randomUUID(), "Page 1", "Alpha beta gamma."),
                new DocumentChunkDto(UUID.randomUUID(), "Page 2", "Delta epsilon zeta."));

        List<DocumentChunkDto> selected = retriever.retrieve(chunks, "completely unrelated question", 3, 5000);

        assertEquals(2, selected.size());
        assertEquals("Page 1", selected.get(0).reference());
    }

    @Test
    void returnsEmptyForAnEmptyDocument() {
        assertTrue(retriever.retrieve(List.of(), "anything", 3, 5000).isEmpty());
    }
}
