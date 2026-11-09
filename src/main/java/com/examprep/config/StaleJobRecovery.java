package com.examprep.config;

import com.examprep.document.domain.Document;
import com.examprep.document.domain.DocumentStatus;
import com.examprep.document.service.DocumentProcessingService;
import com.examprep.document.service.DocumentStateService;
import com.examprep.study.domain.StudyPackage;
import com.examprep.study.service.StudyPackageGenerationService;
import com.examprep.study.service.StudyPackageStateService;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * At startup, unfinished jobs are inspected so students are not left with permanently
 * stuck records. Queued uploads are retried once; an extraction that was mid-flight is
 * marked FAILED with a clear explanation.
 */
@Component
public class StaleJobRecovery implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(StaleJobRecovery.class);

    private final DocumentStateService documentStateService;
    private final DocumentProcessingService documentProcessingService;
    private final StudyPackageStateService studyPackageStateService;
    private final StudyPackageGenerationService studyPackageGenerationService;
    private final AppProperties appProperties;

    public StaleJobRecovery(DocumentStateService documentStateService,
                            DocumentProcessingService documentProcessingService,
                            StudyPackageStateService studyPackageStateService,
                            StudyPackageGenerationService studyPackageGenerationService,
                            AppProperties appProperties) {
        this.documentStateService = documentStateService;
        this.documentProcessingService = documentProcessingService;
        this.studyPackageStateService = studyPackageStateService;
        this.studyPackageGenerationService = studyPackageGenerationService;
        this.appProperties = appProperties;
    }

    @Override
    public void run(ApplicationArguments args) {
        Instant cutoff = Instant.now().minus(Duration.ofMinutes(Math.max(5, appProperties.getProcessing().getStaleJobMinutes())));

        List<Document> documents = documentStateService.findStale(cutoff);
        for (Document document : documents) {
            if (document.getStatus() == DocumentStatus.UPLOADED) {
                log.info("Retrying queued document extraction for {}", document.getId());
                documentProcessingService.process(document.getId());
            } else {
                log.info("Marking interrupted document extraction as failed for {}", document.getId());
                documentStateService.markInterrupted(document.getId());
            }
        }

        List<StudyPackage> studyPackages = studyPackageStateService.findStale(cutoff);
        for (StudyPackage studyPackage : studyPackages) {
            log.info("Marking interrupted study package generation as failed for {}", studyPackage.getId());
            studyPackageStateService.markInterrupted(studyPackage.getId());
        }
    }
}
