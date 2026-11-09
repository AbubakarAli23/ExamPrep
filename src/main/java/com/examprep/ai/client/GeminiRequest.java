package com.examprep.ai.client;

/**
 * One Gemini generation request. The API key never appears here; it is read once by the
 * client from configuration.
 */
public record GeminiRequest(
        String systemInstruction,
        String userPrompt,
        boolean jsonOutput,
        Integer maxOutputTokens,
        Double temperature) {

    public static GeminiRequest text(String systemInstruction, String userPrompt) {
        return new GeminiRequest(systemInstruction, userPrompt, false, null, 0.2);
    }

    public static GeminiRequest json(String systemInstruction, String userPrompt, double temperature) {
        return new GeminiRequest(systemInstruction, userPrompt, true, null, temperature);
    }
}
