package com.examprep.document.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

/**
 * A contiguous piece of extracted document text with a stable, human readable source
 * reference such as "Page 12" or "Slide 4".
 */
@Entity
@Table(name = "document_chunks")
public class DocumentChunk {

    @Id
    private UUID id;

    @Column(name = "document_id", nullable = false)
    private UUID documentId;

    @Column(name = "chunk_index", nullable = false)
    private int chunkIndex;

    @Column(name = "chunk_text", nullable = false, columnDefinition = "text")
    private String text;

    @Column(name = "source_reference", nullable = false, length = 160)
    private String sourceReference;

    @Column(name = "character_count", nullable = false)
    private int characterCount;

    protected DocumentChunk() {
    }

    public DocumentChunk(UUID id, UUID documentId, int chunkIndex, String text, String sourceReference) {
        this.id = id;
        this.documentId = documentId;
        this.chunkIndex = chunkIndex;
        this.text = text;
        this.sourceReference = sourceReference;
        this.characterCount = text.length();
    }

    public UUID getId() {
        return id;
    }

    public UUID getDocumentId() {
        return documentId;
    }

    public int getChunkIndex() {
        return chunkIndex;
    }

    public String getText() {
        return text;
    }

    public String getSourceReference() {
        return sourceReference;
    }

    public int getCharacterCount() {
        return characterCount;
    }
}
