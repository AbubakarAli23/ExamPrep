package com.examprep.ai.exception;

/**
 * Raised when AI features are used but Gemini has not been configured. Never replaced by
 * fake generated content.
 */
public class AiConfigurationException extends RuntimeException {

    public AiConfigurationException(String message) {
        super(message);
    }

    public static AiConfigurationException missingKey() {
        return new AiConfigurationException(
                "The Gemini API key is not configured on this server. "
                        + "Set GOOGLE_API_KEY and restart ExamPrep to use study generation and the tutor.");
    }
}
