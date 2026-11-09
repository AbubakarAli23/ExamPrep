package com.examprep.study.service;

import com.examprep.ai.config.AiProperties;
import com.examprep.ai.exception.AiConfigurationException;
import com.examprep.ai.exception.AiResponseException;
import com.examprep.ai.exception.AiServiceException;
import com.examprep.ai.service.StudyContentGenerator;
import com.examprep.document.dto.DocumentDtos.DocumentContext;
import com.examprep.document.service.DocumentApplicationService;
import com.examprep.study.domain.StudyPackage;
import com.examprep.study.domain.content.StudyContent;
import com.examprep.study.dto.StudyPreferences;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Background generation worker. Every failure becomes a clear FAILED state, never a fake
 * package.
 */
@Service
public class StudyPackageGenerationService {

    private static final Logger log = LoggerFactory.getLogger(StudyPackageGenerationService.class);

    private final StudyPackageStateService stateService;
    private final DocumentApplicationService documentApplicationService;
    private final StudyContentGenerator contentGenerator;
    private final AiProperties aiProperties;

    public StudyPackageGenerationService(StudyPackageStateService stateService,
                                         DocumentApplicationService documentApplicationService,
                                         StudyContentGenerator contentGenerator,
                                         AiProperties aiProperties) {
        this.stateService = stateService;
        this.documentApplicationService = documentApplicationService;
        this.contentGenerator = contentGenerator;
        this.aiProperties = aiProperties;
    }

    @Async("studyTaskExecutor")
    public void generate(UUID studyPackId) {
        StudyPackage studyPackage = stateService.start(studyPackId);
        if (studyPackage == null) {
            log.warn("Study package {} disappeared before generation started", studyPackId);
            return;
        }
        try {
            DocumentContext context = documentApplicationService.getContext(
                    studyPackage.getDocumentId(), studyPackage.getOwnerId());
            StudyPreferences preferences = new StudyPreferences(
                    studyPackage.getLanguage(),
                    studyPackage.getAcademicLevel(),
                    studyPackage.getExamDate(),
                    studyPackage.getRequestedQuestionCount(),
                    studyPackage.getRequestedMockQuestionCount(),
                    studyPackage.getRequestedFlashcardCount()).normalized();

            StudyContent.GeneratedPackage generated = contentGenerator.generate(
                    context,
                    preferences,
                    message -> stateService.updateProgress(studyPackId, message));

            stateService.complete(studyPackId, generated, aiProperties.getModel());
            log.info("Study package {} generated with {} flashcards", studyPackId, generated.flashcards().size());
        } catch (AiConfigurationException ex) {
            stateService.fail(studyPackId, ex.getMessage());
        } catch (AiResponseException ex) {
            stateService.fail(studyPackId,
                    "Gemini's response could not be turned into a complete study package. " + ex.getMessage());
        } catch (AiServiceException ex) {
            stateService.fail(studyPackId, ex.getMessage());
        } catch (Exception ex) {
            log.error("Unexpected failure generating study package {}", studyPackId, ex);
            stateService.fail(studyPackId,
                    "This study package could not be generated. Please try again from the document page.");
        }
    }
}
