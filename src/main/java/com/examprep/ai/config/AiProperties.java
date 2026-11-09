package com.examprep.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Gemini settings. Every value can be supplied through the environment; nothing is
 * hardcoded in the AI services.
 */
@ConfigurationProperties(prefix = "app.ai")
public class AiProperties {

    /** Gemini Developer API key. Empty means AI features are disabled, not faked. */
    private String apiKey = "";

    /** Gemini model identifier, e.g. gemini-3.8-flash. */
    private String model = "gemini-3.8-flash";

    private int requestTimeoutSeconds = 150;

    private int maxRetries = 2;

    private int maxOutputTokens = 16384;

    /** How much document text one analysis request may carry. */
    private int batchCharacterLimit = 14000;

    /** How much material the final synthesis request may carry. */
    private int synthesisCharacterLimit = 26000;

    /** Number of source chunks retrieved for each tutor question. */
    private int tutorContextChunks = 6;

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public int getRequestTimeoutSeconds() {
        return requestTimeoutSeconds;
    }

    public void setRequestTimeoutSeconds(int requestTimeoutSeconds) {
        this.requestTimeoutSeconds = requestTimeoutSeconds;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    public int getMaxOutputTokens() {
        return maxOutputTokens;
    }

    public void setMaxOutputTokens(int maxOutputTokens) {
        this.maxOutputTokens = maxOutputTokens;
    }

    public int getBatchCharacterLimit() {
        return batchCharacterLimit;
    }

    public void setBatchCharacterLimit(int batchCharacterLimit) {
        this.batchCharacterLimit = batchCharacterLimit;
    }

    public int getSynthesisCharacterLimit() {
        return synthesisCharacterLimit;
    }

    public void setSynthesisCharacterLimit(int synthesisCharacterLimit) {
        this.synthesisCharacterLimit = synthesisCharacterLimit;
    }

    public int getTutorContextChunks() {
        return tutorContextChunks;
    }

    public void setTutorContextChunks(int tutorContextChunks) {
        this.tutorContextChunks = tutorContextChunks;
    }
}
