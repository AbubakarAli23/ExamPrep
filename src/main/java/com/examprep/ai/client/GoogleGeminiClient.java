package com.examprep.ai.client;

import com.examprep.ai.config.AiProperties;
import com.examprep.ai.exception.AiConfigurationException;
import com.examprep.ai.exception.AiServiceException;
import com.google.genai.Client;
import com.google.genai.errors.ApiException;
import com.google.genai.errors.GenAiIOException;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.HttpOptions;
import com.google.genai.types.Part;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Service;

/**
 * Official Google Gen AI Java SDK adapter for the Gemini Developer API. The API key is read
 * from configuration only; it is never logged or returned to the browser.
 */
@Service
public class GoogleGeminiClient implements GeminiClient, DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(GoogleGeminiClient.class);

    private final AiProperties aiProperties;
    private volatile Client client;

    public GoogleGeminiClient(AiProperties aiProperties) {
        this.aiProperties = aiProperties;
    }

    @Override
    public String generate(GeminiRequest request) {
        Client gemini = client();
        GenerateContentConfig.Builder configBuilder = GenerateContentConfig.builder()
                .systemInstruction(Content.fromParts(Part.fromText(request.systemInstruction())))
                .httpOptions(HttpOptions.builder()
                        .timeout(Math.max(5, aiProperties.getRequestTimeoutSeconds()) * 1000)
                        .build())
                .maxOutputTokens(request.maxOutputTokens() == null
                        ? aiProperties.getMaxOutputTokens()
                        : request.maxOutputTokens());
        if (request.temperature() != null) {
            configBuilder.temperature(request.temperature().floatValue());
        }
        if (request.jsonOutput()) {
            configBuilder.responseMimeType("application/json");
        }
        GenerateContentConfig config = configBuilder.build();

        int attempts = Math.max(0, aiProperties.getMaxRetries()) + 1;
        for (int attempt = 1; attempt <= attempts; attempt++) {
            long started = System.nanoTime();
            try {
                GenerateContentResponse response = gemini.models.generateContent(
                        aiProperties.getModel(), request.userPrompt(), config);
                String text = response.text();
                long millis = (System.nanoTime() - started) / 1_000_000;
                log.info("Gemini call finished model={} attempt={} durationMs={} characters={}",
                        aiProperties.getModel(), attempt, millis, text == null ? 0 : text.length());
                if (text == null || text.isBlank()) {
                    throw new AiServiceException(
                            "Gemini returned an empty response. Please try again in a moment.");
                }
                return text;
            } catch (ApiException ex) {
                int status = ex.code();
                log.warn("Gemini API error status={} attempt={} statusText={}",
                        status, attempt, ex.status());
                if (isRetryable(status) && attempt < attempts) {
                    sleep(backoffMillis(attempt));
                    continue;
                }
                throw new AiServiceException(messageFor(status), ex);
            } catch (GenAiIOException ex) {
                log.warn("Gemini transport failure attempt={}: {}", attempt, ex.getClass().getSimpleName());
                if (attempt < attempts) {
                    sleep(backoffMillis(attempt));
                    continue;
                }
                throw new AiServiceException(
                        "ExamPrep could not reach Gemini. Check the server's network connection and try again.", ex);
            } catch (AiServiceException ex) {
                throw ex;
            } catch (RuntimeException ex) {
                log.error("Unexpected Gemini failure attempt={}", attempt, ex);
                if (attempt < attempts) {
                    sleep(backoffMillis(attempt));
                    continue;
                }
                throw new AiServiceException("Gemini could not complete this request. Please try again.", ex);
            }
        }
        throw new AiServiceException("Gemini could not complete this request. Please try again.");
    }

    private Client client() {
        if (!aiProperties.isConfigured()) {
            throw AiConfigurationException.missingKey();
        }
        Client existing = client;
        if (existing == null) {
            synchronized (this) {
                existing = client;
                if (existing == null) {
                    existing = Client.builder().apiKey(aiProperties.getApiKey().trim()).build();
                    client = existing;
                }
            }
        }
        return existing;
    }

    private boolean isRetryable(int status) {
        return status == 429 || status == 500 || status == 502 || status == 503 || status == 504;
    }

    private long backoffMillis(int attempt) {
        return 1000L * (1L << Math.min(attempt - 1, 2));
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new AiServiceException("The AI request was interrupted. Please try again.", ex);
        }
    }

    private String messageFor(int status) {
        return switch (status) {
            case 400, 403 -> "Gemini rejected the request. Check that GOOGLE_API_KEY is valid and has access to "
                    + "the configured model (" + aiProperties.getModel() + ").";
            case 404 -> "The configured Gemini model (" + aiProperties.getModel()
                    + ") was not found. Update GEMINI_MODEL to a model your key can use.";
            case 429 -> "Gemini's rate limit was reached. Wait a moment and try again.";
            default -> "Gemini is temporarily unavailable (status " + status + "). Please try again later.";
        };
    }

    @Override
    public void destroy() {
        Client existing = client;
        if (existing != null) {
            try {
                existing.close();
            } catch (Exception ex) {
                log.debug("Gemini client close failed", ex);
            }
        }
    }
}
