package com.examprep.ai.exception;

/**
 * Provider, network or rate-limit failure. The message is safe to show to students.
 */
public class AiServiceException extends RuntimeException {

    public AiServiceException(String message) {
        super(message);
    }

    public AiServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
