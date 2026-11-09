package com.examprep.ai.client;

/**
 * Gateway boundary: business code depends on this interface, never on the Google SDK.
 */
public interface GeminiClient {

    /**
     * Generates text or JSON according to the request.
     *
     * @throws com.examprep.ai.exception.AiConfigurationException when no API key is configured
     * @throws com.examprep.ai.exception.AiServiceException for provider or network failures
     */
    String generate(GeminiRequest request);
}
