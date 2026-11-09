package com.examprep.ai.service;

import com.examprep.ai.client.GeminiClient;
import com.examprep.ai.client.GeminiRequest;
import com.examprep.ai.config.AiProperties;
import com.examprep.ai.dto.AiStudyContent;
import com.examprep.ai.exception.AiResponseException;
import com.examprep.ai.prompt.PromptLibrary;
import com.examprep.ai.validation.CitationValidator;
import com.examprep.document.dto.DocumentDtos.DocumentChunkDto;
import com.examprep.document.dto.DocumentDtos.DocumentContext;
import com.examprep.study.domain.content.StudyContent;
import com.examprep.study.dto.StudyPreferences;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Hierarchical generation: analyse the document in bounded batches, summarise each batch,
 * then synthesise one study package from the summaries plus representative excerpts.
 */
@Service
public class GeminiStudyContentGenerator implements StudyContentGenerator {

    private static final Logger log = LoggerFactory.getLogger(GeminiStudyContentGenerator.class);

    private final GeminiClient geminiClient;
    private final PromptLibrary promptLibrary;
    private final StudyContentAssembler assembler;
    private final AiProperties aiProperties;
    private final ObjectMapper objectMapper;

    public GeminiStudyContentGenerator(GeminiClient geminiClient,
                                       PromptLibrary promptLibrary,
                                       StudyContentAssembler assembler,
                                       AiProperties aiProperties,
                                       ObjectMapper objectMapper) {
        this.geminiClient = geminiClient;
        this.promptLibrary = promptLibrary;
        this.assembler = assembler;
        this.aiProperties = aiProperties;
        this.objectMapper = objectMapper;
    }

    @Override
    public StudyContent.GeneratedPackage generate(DocumentContext document,
                                                  StudyPreferences preferences,
                                                  Consumer<String> progress) {
        if (document.chunks().isEmpty()) {
            throw new AiResponseException("This document has no extracted text to study.");
        }
        Set<String> allowedReferences = allowedReferences(document.chunks());
        List<List<DocumentChunkDto>> batches = batches(document.chunks(), aiProperties.getBatchCharacterLimit());
        progress.accept(batches.size() == 1
                ? "Analysing the document."
                : "Analysing the document in " + batches.size() + " sections.");

        List<String> materialSections = new ArrayList<>();
        if (batches.size() > 1) {
            for (int index = 0; index < batches.size(); index++) {
                List<DocumentChunkDto> batch = batches.get(index);
                progress.accept("Summarising section " + (index + 1) + " of " + batches.size() + ".");
                String summaryJson = geminiClient.generate(GeminiRequest.json(
                        promptLibrary.systemInstruction(),
                        promptLibrary.sectionSummaryPrompt(renderMaterial(batch), referencesOf(batch)),
                        0.2));
                AiStudyContent.AiSectionSummary summary = parse(summaryJson, AiStudyContent.AiSectionSummary.class);
                if (summary != null && summary.summary() != null && !summary.summary().isBlank()) {
                    materialSections.add("Section " + (index + 1) + " summary:\n" + summary.summary().strip()
                            + keyPoints(summary));
                }
            }
        }
        materialSections.addAll(representativeExcerpts(document.chunks(), aiProperties.getSynthesisCharacterLimit()));

        progress.accept("Writing your revision notes, flashcards and questions.");
        StudyContent.GeneratedPackage generated = null;
        AiResponseException lastFailure = null;
        for (int attempt = 1; attempt <= 2; attempt++) {
            String prompt = promptLibrary.studyPackagePrompt(materialSections, allowedReferences, preferences);
            String json = geminiClient.generate(GeminiRequest.json(
                    promptLibrary.systemInstruction(), prompt, 0.25));
            try {
                AiStudyContent raw = parse(json, AiStudyContent.class);
                generated = assembler.assemble(raw, allowedReferences, preferences, document.title());
                break;
            } catch (AiResponseException ex) {
                lastFailure = ex;
                log.warn("Study generation attempt {} produced invalid content: {}", attempt, ex.getMessage());
                if (attempt == 2) {
                    throw new AiResponseException(
                            "Gemini's response could not be validated: " + ex.getMessage()
                                    + " Please try generating the package again.", ex);
                }
                progress.accept("The first draft was incomplete; asking Gemini for a complete package.");
            }
        }
        if (generated == null) {
            throw lastFailure == null
                    ? new AiResponseException("Gemini did not return a usable study package.")
                    : lastFailure;
        }
        return generated;
    }

    private String keyPoints(AiStudyContent.AiSectionSummary summary) {
        if (summary.keyPoints() == null || summary.keyPoints().isEmpty()) {
            return "";
        }
        return "\nKey points:\n- " + String.join("\n- ", summary.keyPoints().stream()
                .filter(point -> point != null && !point.isBlank())
                .limit(8)
                .toList());
    }

    private List<List<DocumentChunkDto>> batches(List<DocumentChunkDto> chunks, int characterLimit) {
        List<List<DocumentChunkDto>> batches = new ArrayList<>();
        List<DocumentChunkDto> current = new ArrayList<>();
        int currentSize = 0;
        for (DocumentChunkDto chunk : chunks) {
            int size = chunk.text().length();
            if (!current.isEmpty() && currentSize + size > Math.max(2000, characterLimit)) {
                batches.add(current);
                current = new ArrayList<>();
                currentSize = 0;
            }
            current.add(chunk);
            currentSize += size;
        }
        if (!current.isEmpty()) {
            batches.add(current);
        }
        return batches;
    }

    private List<String> representativeExcerpts(List<DocumentChunkDto> chunks, int characterBudget) {
        List<String> excerpts = new ArrayList<>();
        int used = 0;
        int budget = Math.max(4000, characterBudget);
        int step = Math.max(1, chunks.size() / 12);
        for (int index = 0; index < chunks.size() && used < budget; index += step) {
            DocumentChunkDto chunk = chunks.get(index);
            int remaining = budget - used;
            String text = chunk.text().length() > remaining
                    ? chunk.text().substring(0, remaining)
                    : chunk.text();
            excerpts.add("Excerpt [source: " + chunk.reference() + "]\n" + text);
            used += text.length();
        }
        return excerpts;
    }

    private String renderMaterial(List<DocumentChunkDto> batch) {
        StringBuilder material = new StringBuilder();
        for (DocumentChunkDto chunk : batch) {
            material.append("[source: ").append(chunk.reference()).append("]\n")
                    .append(chunk.text()).append("\n\n");
        }
        return material.toString();
    }

    private Set<String> referencesOf(List<DocumentChunkDto> chunks) {
        Set<String> references = new LinkedHashSet<>();
        chunks.forEach(chunk -> references.add(chunk.reference()));
        return references;
    }

    private Set<String> allowedReferences(List<DocumentChunkDto> chunks) {
        return CitationValidator.allowedReferences(chunks.stream().map(DocumentChunkDto::reference).toList());
    }

    private <T> T parse(String json, Class<T> type) {
        String cleaned = stripFences(json);
        try {
            T parsed = objectMapper.readValue(cleaned, type);
            if (parsed == null) {
                throw new AiResponseException("Gemini returned no structured content.");
            }
            return parsed;
        } catch (AiResponseException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new AiResponseException("Gemini returned malformed JSON that could not be read.", ex);
        }
    }

    private String stripFences(String json) {
        String cleaned = json == null ? "" : json.strip();
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.replaceFirst("^```[a-zA-Z]*\\s*", "");
            if (cleaned.endsWith("```")) {
                cleaned = cleaned.substring(0, cleaned.length() - 3);
            }
        }
        return cleaned.strip();
    }
}
