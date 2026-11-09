package com.examprep.study.service;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class StudyPackageGenerationListener {

    private final StudyPackageGenerationService generationService;

    public StudyPackageGenerationListener(StudyPackageGenerationService generationService) {
        this.generationService = generationService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onStudyPackageRequested(StudyPackageRequestedEvent event) {
        generationService.generate(event.studyPackId());
    }
}
