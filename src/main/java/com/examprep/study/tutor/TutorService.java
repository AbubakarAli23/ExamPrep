package com.examprep.study.tutor;

import com.examprep.ai.client.GeminiClient;
import com.examprep.ai.client.GeminiRequest;
import com.examprep.ai.config.AiProperties;
import com.examprep.ai.exception.AiResponseException;
import com.examprep.ai.prompt.PromptLibrary;
import com.examprep.ai.retrieval.ChunkRetriever;
import com.examprep.ai.validation.CitationValidator;
import com.examprep.common.exception.InvalidRequestException;
import com.examprep.common.exception.ResourceNotFoundException;
import com.examprep.common.web.DisplayDates;
import com.examprep.document.dto.DocumentDtos.DocumentChunkDto;
import com.examprep.document.dto.DocumentDtos.DocumentContext;
import com.examprep.document.service.DocumentApplicationService;
import com.examprep.study.domain.MessageRole;
import com.examprep.study.domain.StudyPackage;
import com.examprep.study.domain.StudyPackageStatus;
import com.examprep.study.domain.TutorMessage;
import com.examprep.study.dto.StudyPackDtos.TutorAnswerDto;
import com.examprep.study.dto.StudyPackDtos.TutorMessageView;
import com.examprep.study.dto.StudyPackDtos.TutorQuestionDto;
import com.examprep.study.repository.StudyPackageRepository;
import com.examprep.study.repository.TutorMessageRepository;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The study tutor: retrieves the most relevant source chunks, asks Gemini to answer only
 * from them, validates citations, and keeps the conversation for this package.
 */
@Service
public class TutorService {

    private static final Logger log = LoggerFactory.getLogger(TutorService.class);
    private static final int MAX_QUESTION_LENGTH = 2000;
    private static final int CONTEXT_CHARACTER_BUDGET = 14000;
    private static final int HISTORY_MESSAGES = 8;

    private final StudyPackageRepository studyPackageRepository;
    private final TutorMessageRepository tutorMessageRepository;
    private final DocumentApplicationService documentApplicationService;
    private final ChunkRetriever chunkRetriever;
    private final GeminiClient geminiClient;
    private final PromptLibrary promptLibrary;
    private final AiProperties aiProperties;
    private final ObjectMapper objectMapper;

    public TutorService(StudyPackageRepository studyPackageRepository,
                        TutorMessageRepository tutorMessageRepository,
                        DocumentApplicationService documentApplicationService,
                        ChunkRetriever chunkRetriever,
                        GeminiClient geminiClient,
                        PromptLibrary promptLibrary,
                        AiProperties aiProperties,
                        ObjectMapper objectMapper) {
        this.studyPackageRepository = studyPackageRepository;
        this.tutorMessageRepository = tutorMessageRepository;
        this.documentApplicationService = documentApplicationService;
        this.chunkRetriever = chunkRetriever;
        this.geminiClient = geminiClient;
        this.promptLibrary = promptLibrary;
        this.aiProperties = aiProperties;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<TutorMessageView> history(UUID studyPackId, UUID ownerId) {
        requirePackage(studyPackId, ownerId);
        return tutorMessageRepository.findByStudyPackageIdOrderByCreatedAtAscIdAsc(studyPackId).stream()
                .map(message -> new TutorMessageView(
                        message.getRole().name(),
                        message.getContent(),
                        message.getSourceReferences() == null ? List.of() : message.getSourceReferences(),
                        DisplayDates.dateTime(message.getCreatedAt())))
                .toList();
    }

    @Transactional
    public TutorAnswerDto ask(UUID studyPackId, UUID ownerId, TutorQuestionDto questionDto) {
        StudyPackage studyPackage = requirePackage(studyPackId, ownerId);
        if (studyPackage.getStatus() != StudyPackageStatus.READY) {
            throw new InvalidRequestException("The tutor is available once your study package is ready.");
        }
        String question = questionDto == null ? null : questionDto.question();
        if (question == null || question.isBlank()) {
            throw new InvalidRequestException("Please type a question for the tutor.");
        }
        question = question.strip();
        if (question.length() > MAX_QUESTION_LENGTH) {
            throw new InvalidRequestException("Please keep your question under 2,000 characters.");
        }

        DocumentContext document = documentApplicationService.getContext(
                studyPackage.getDocumentId(), ownerId);
        List<DocumentChunkDto> contextChunks = chunkRetriever.retrieve(
                document.chunks(),
                question,
                Math.max(1, aiProperties.getTutorContextChunks()),
                CONTEXT_CHARACTER_BUDGET);
        if (contextChunks.isEmpty()) {
            throw new InvalidRequestException("This document has no extracted text for the tutor to use.");
        }

        Set<String> allowedReferences = CitationValidator.allowedReferences(
                document.chunks().stream().map(DocumentChunkDto::reference).toList());
        List<PromptLibrary.TutorTurn> history = recentHistory(studyPackId);
        String prompt = promptLibrary.tutorPrompt(contextChunks, history, question, allowedReferences);

        String json = geminiClient.generate(GeminiRequest.json(promptLibrary.systemInstruction(), prompt, 0.3));
        TutorPayload payload = parse(json);
        if (payload.answer() == null || payload.answer().isBlank()) {
            throw new AiResponseException("The tutor returned an empty answer. Please try again.");
        }
        List<String> references = CitationValidator.filter(payload.sourceReferences(), allowedReferences);

        Instant now = Instant.now();
        tutorMessageRepository.save(new TutorMessage(
                UUID.randomUUID(), ownerId, studyPackId, MessageRole.USER, question, List.of(), now));
        tutorMessageRepository.save(new TutorMessage(
                UUID.randomUUID(), ownerId, studyPackId, MessageRole.ASSISTANT,
                payload.answer().strip(), references, now.plusMillis(1)));

        log.info("Tutor answered study package {} with {} validated sources", studyPackId, references.size());
        return new TutorAnswerDto(
                payload.answer().strip(),
                references,
                Boolean.TRUE.equals(payload.insufficientInformation()),
                DisplayDates.dateTime(now));
    }

    private List<PromptLibrary.TutorTurn> recentHistory(UUID studyPackId) {
        List<TutorMessage> messages = new ArrayList<>(
                tutorMessageRepository.findTop10ByStudyPackageIdOrderByCreatedAtDesc(studyPackId));
        Collections.reverse(messages);
        int start = Math.max(0, messages.size() - HISTORY_MESSAGES);
        return messages.subList(start, messages.size()).stream()
                .map(message -> new PromptLibrary.TutorTurn(
                        message.getRole() == MessageRole.USER ? "Student" : "Tutor",
                        message.getContent()))
                .toList();
    }

    private TutorPayload parse(String json) {
        String cleaned = json == null ? "" : json.strip();
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.replaceFirst("^```[a-zA-Z]*\\s*", "");
            if (cleaned.endsWith("```")) {
                cleaned = cleaned.substring(0, cleaned.length() - 3);
            }
        }
        try {
            TutorPayload payload = objectMapper.readValue(cleaned.strip(), TutorPayload.class);
            if (payload == null) {
                throw new AiResponseException("The tutor returned no usable answer.");
            }
            return payload;
        } catch (AiResponseException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new AiResponseException("The tutor's answer could not be read. Please try again.", ex);
        }
    }

    private StudyPackage requirePackage(UUID studyPackId, UUID ownerId) {
        return studyPackageRepository.findByIdAndOwnerId(studyPackId, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("That study package could not be found."));
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record TutorPayload(String answer, List<String> sourceReferences, Boolean insufficientInformation) {
    }
}
