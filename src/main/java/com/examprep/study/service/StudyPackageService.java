package com.examprep.study.service;

import com.examprep.common.exception.InvalidRequestException;
import com.examprep.common.exception.ResourceNotFoundException;
import com.examprep.document.dto.DocumentDtos.DocumentStatusDto;
import com.examprep.document.service.DocumentApplicationService;
import com.examprep.study.domain.QuestionSection;
import com.examprep.study.domain.StudyPackage;
import com.examprep.study.dto.StudyPackDtos.AttemptSummaryDto;
import com.examprep.study.dto.StudyPackDtos.QuizResultDto;
import com.examprep.study.dto.StudyPackDtos.QuizSubmissionDto;
import com.examprep.study.dto.StudyPackDtos.StudyPackCreationResult;
import com.examprep.study.dto.StudyPackDtos.StudyPackDto;
import com.examprep.study.dto.StudyPackDtos.StudyPackStatusDto;
import com.examprep.study.dto.StudyPackDtos.StudyPackSummaryDto;
import com.examprep.study.dto.StudyPackDtos.StudyProgressDto;
import com.examprep.study.dto.StudyPackDtos.TutorAnswerDto;
import com.examprep.study.dto.StudyPackDtos.TutorQuestionDto;
import com.examprep.study.dto.StudyPreferences;
import com.examprep.study.quiz.QuizService;
import com.examprep.study.repository.FlashcardRepository;
import com.examprep.study.repository.FlashcardReviewRepository;
import com.examprep.study.repository.QuizQuestionRepository;
import com.examprep.study.repository.StudyPackageRepository;
import com.examprep.study.tutor.TutorService;
import java.util.List;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StudyPackageService implements StudyPackApplicationService {

    private final StudyPackageRepository studyPackageRepository;
    private final FlashcardRepository flashcardRepository;
    private final QuizQuestionRepository quizQuestionRepository;
    private final FlashcardReviewRepository flashcardReviewRepository;
    private final DocumentApplicationService documentApplicationService;
    private final StudyPackageMapper mapper;
    private final ApplicationEventPublisher eventPublisher;
    private final QuizService quizService;
    private final TutorService tutorService;

    public StudyPackageService(StudyPackageRepository studyPackageRepository,
                               FlashcardRepository flashcardRepository,
                               QuizQuestionRepository quizQuestionRepository,
                               FlashcardReviewRepository flashcardReviewRepository,
                               DocumentApplicationService documentApplicationService,
                               StudyPackageMapper mapper,
                               ApplicationEventPublisher eventPublisher,
                               QuizService quizService,
                               TutorService tutorService) {
        this.studyPackageRepository = studyPackageRepository;
        this.flashcardRepository = flashcardRepository;
        this.quizQuestionRepository = quizQuestionRepository;
        this.flashcardReviewRepository = flashcardReviewRepository;
        this.documentApplicationService = documentApplicationService;
        this.mapper = mapper;
        this.eventPublisher = eventPublisher;
        this.quizService = quizService;
        this.tutorService = tutorService;
    }

    @Override
    @Transactional
    public StudyPackCreationResult create(UUID documentId, UUID ownerId, StudyPreferences preferences) {
        DocumentStatusDto document = documentApplicationService.getStatus(documentId, ownerId);
        if (!document.ready()) {
            if (document.failureReason() != null && !document.failureReason().isBlank()) {
                throw new InvalidRequestException(document.failureReason());
            }
            throw new InvalidRequestException(
                    "This document is still being extracted. You can generate a study package once it is ready.");
        }

        StudyPreferences normalized = (preferences == null ? StudyPreferences.defaults() : preferences).normalized();
        StudyPackage studyPackage = new StudyPackage(
                UUID.randomUUID(),
                ownerId,
                documentId,
                normalized.language(),
                normalized.academicLevel(),
                normalized.examDate(),
                normalized.practiceQuestionCount(),
                normalized.mockQuestionCount(),
                normalized.flashcardCount());
        studyPackageRepository.save(studyPackage);
        eventPublisher.publishEvent(new StudyPackageRequestedEvent(studyPackage.getId()));

        return new StudyPackCreationResult(
                studyPackage.getId(),
                studyPackage.getStatus().name(),
                "Generation started. ExamPrep will build your study package in the background.");
    }

    @Override
    @Transactional(readOnly = true)
    public StudyPackStatusDto getStatus(UUID studyPackId, UUID ownerId) {
        return mapper.toStatus(requirePackage(studyPackId, ownerId));
    }

    @Override
    @Transactional(readOnly = true)
    public StudyPackDto getStudyPack(UUID studyPackId, UUID ownerId) {
        StudyPackage studyPackage = requirePackage(studyPackId, ownerId);
        String documentTitle = documentApplicationService.getStatus(studyPackage.getDocumentId(), ownerId)
                .originalFilename();
        return mapper.toDto(
                studyPackage,
                documentTitle,
                flashcardRepository.countByStudyPackageId(studyPackId),
                quizQuestionRepository.countByStudyPackageIdAndSection(studyPackId, QuestionSection.PRACTICE),
                quizQuestionRepository.countByStudyPackageIdAndSection(studyPackId, QuestionSection.MOCK));
    }

    @Override
    @Transactional(readOnly = true)
    public List<StudyPackSummaryDto> listRecent(UUID ownerId) {
        return studyPackageRepository.findTop5ByOwnerIdOrderByCreatedAtDesc(ownerId).stream()
                .map(studyPackage -> {
                    String documentTitle = documentApplicationService
                            .getStatus(studyPackage.getDocumentId(), ownerId)
                            .originalFilename();
                    return mapper.toSummary(
                            studyPackage,
                            documentTitle,
                            flashcardRepository.countByStudyPackageId(studyPackage.getId()),
                            quizQuestionRepository.countByStudyPackageIdAndSection(
                                    studyPackage.getId(), QuestionSection.PRACTICE),
                            quizQuestionRepository.countByStudyPackageIdAndSection(
                                    studyPackage.getId(), QuestionSection.MOCK));
                })
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttemptSummaryDto> recentAttempts(UUID ownerId) {
        return quizService.recentAttempts(ownerId);
    }

    @Override
    @Transactional(readOnly = true)
    public StudyProgressDto progress(UUID ownerId) {
        QuizService.ScoreStats stats = quizService.scoreStats(ownerId);
        return new StudyProgressDto(
                (int) studyPackageRepository.countByOwnerId(ownerId),
                stats.completedExams(),
                stats.averageScorePercent(),
                stats.bestScorePercent(),
                flashcardReviewRepository.sumReviewedCount(ownerId),
                documentApplicationService.listDocuments(ownerId).size());
    }

    @Override
    public QuizResultDto submitQuiz(UUID attemptId, UUID ownerId, QuizSubmissionDto submission) {
        return quizService.submitAttempt(attemptId, ownerId, submission);
    }

    @Override
    public TutorAnswerDto askTutor(UUID studyPackId, UUID ownerId, TutorQuestionDto question) {
        return tutorService.ask(studyPackId, ownerId, question);
    }

    @Override
    @Transactional
    public void delete(UUID studyPackId, UUID ownerId) {
        StudyPackage studyPackage = requirePackage(studyPackId, ownerId);
        studyPackageRepository.delete(studyPackage);
    }

    private StudyPackage requirePackage(UUID studyPackId, UUID ownerId) {
        return studyPackageRepository.findByIdAndOwnerId(studyPackId, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("That study package could not be found."));
    }
}
