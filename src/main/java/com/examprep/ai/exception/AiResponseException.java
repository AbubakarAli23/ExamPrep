package com.examprep.ai.exception;

/**
 * The provider answered, but the content could not be validated. Retried once, then shown
 * as a clear failure instead of being saved.
 */
public class AiResponseException extends RuntimeException {

    public AiResponseException(String message) {
        super(message);
    }

    public AiResponseException(String message, Throwable cause) {
        super(message, cause);
    }
}
