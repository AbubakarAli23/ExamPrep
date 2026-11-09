package com.examprep.document.service;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Emitted after an upload is committed so that extraction never races the transaction.
 */
public record DocumentUploadedEvent(UUID documentId, UUID ownerId) {
}
