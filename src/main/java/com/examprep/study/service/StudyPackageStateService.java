package com.examprep.study.service;

import com.examprep.study.domain.Flashcard;
import com.examprep.study.domain.QuestionSection;
import com.examprep.study.domain.QuizQuestion;
import com.examprep.study.domain.StudyPackage;
import com.examprep.study.domain.StudyPackageStatus;
import com.examprep.study.domain.content.StudyContent;
import com.examprep.study.repository.FlashcardRepository;
import com.examprep.study.repository.QuizQuestionRepository;
import com.examprep.study.repository.StudyPackageRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Transactional state changes for background generation, so polling sees progress without
 * exposing a half-written package.
 */
@Service
public class StudyPackageStateService {

    private static final Logger log = LoggerFactory.getLogger(StudyPackageStateService.class);

    private final StudyPackageRepository studyPackageRepository;
    private final FlashcardRepository flashcardRepository;
    private final QuizQuestionRepository quizQuestionRepository;

    public StudyPackageStateService(StudyPackageRepository studyPackageRepository,
                                    FlashcardRepository flashcardRepository,
                                    QuizQuestionRepository quizQuestionRepository) {
        this.studyPackageRepository = studyPackageRepository;
        this.flashcardRepository = flashcardRepository;
        this.quizQuestionRepository = quizQuestionRepository;
    }

    @Transactional
    public StudyPackage start(UUID studyPackId) {
        StudyPackage studyPackage = studyPackageRepository.findById(studyPackId).orElse(null);
        if (studyPackage == null) {
            return null;
        }
        studyPackage.setStatus(StudyPackageStatus.PROCESSING);
        studyPackage.setProgressMessage("Reading the extracted document material.");
        studyPackage.setFailureReason(null);
        return studyPackageRepository.save(studyPackage);
    }

    @Transactional
    public void updateProgress(UUID studyPackId, String message) {
        studyPackageRepository.findById(studyPackId).ifPresent(studyPackage -> {
            if (studyPackage.getStatus() == StudyPackageStatus.PROCESSING) {
                studyPackage.setProgressMessage(truncate(message, 290));
                studyPackageRepository.save(studyPackage);
            }
        });
    }

    @Transactional
    public void fail(UUID studyPackId, String reason) {
        studyPackageRepository.findById(studyPackId).ifPresent(studyPackage -> {
            studyPackage.setStatus(StudyPackageStatus.FAILED);
            studyPackage.setFailureReason(truncate(reason, 950));
            studyPackage.setProgressMessage(null);
            studyPackageRepository.save(studyPackage);
            log.info("Study package {} failed: {}", studyPackId, reason);
        });
    }

    /**
     * Persists validated generated content and flips the package to READY atomically, so a
     * partially rendered package can never be observed.
     */
    @Transactional
    public void complete(UUID studyPackId, StudyContent.GeneratedPackage content, String model) {
        StudyPackage studyPackage = studyPackageRepository.findById(studyPackId)
                .orElseThrow(() -> new IllegalStateException("Study package disappeared during generation"));

        studyPackage.setTitle(content.title());
        studyPackage.setIntroduction(content.introduction());
        studyPackage.setOverallSummary(content.overallSummary());
        studyPackage.setLearningObjectives(content.learningObjectives());
        studyPackage.setRevisionPriorities(content.revisionPriorities());
        studyPackage.setTopics(content.topics());
        studyPackage.setRevisionNotes(content.revisionNotes());
        studyPackage.setDefinitions(content.definitions());
        studyPackage.setFormulas(content.formulas());
        studyPackage.setAiModel(model);
        studyPackage.setFailureReason(null);
        studyPackage.setProgressMessage(null);
        studyPackage.setStatus(StudyPackageStatus.READY);
        studyPackage.setCompletedAt(Instant.now());
        studyPackageRepository.save(studyPackage);

        flashcardRepository.deleteByStudyPackageId(studyPackId);
        quizQuestionRepository.deleteByStudyPackageId(studyPackId);
        flashcardRepository.flush();
        quizQuestionRepository.flush();

        List<Flashcard> flashcards = new ArrayList<>();
        int position = 1;
        for (StudyContent.Flashcard card : content.flashcards()) {
            flashcards.add(new Flashcard(UUID.randomUUID(), studyPackId, position++,
                    card.front(), card.back(), card.sourceReference()));
        }
        flashcardRepository.saveAll(flashcards);

        List<QuizQuestion> questions = new ArrayList<>();
        saveQuestions(questions, studyPackId, QuestionSection.PRACTICE, content.practiceQuestions());
        saveQuestions(questions, studyPackId, QuestionSection.MOCK, content.mockQuestions());
        quizQuestionRepository.saveAll(questions);
    }

    private void saveQuestions(List<QuizQuestion> target, UUID studyPackId, QuestionSection section,
                               List<StudyContent.GeneratedQuestion> questions) {
        int position = 1;
        for (StudyContent.GeneratedQuestion question : questions) {
            target.add(new QuizQuestion(
                    UUID.randomUUID(),
                    studyPackId,
                    section,
                    position++,
                    question.type(),
                    question.prompt(),
                    question.options(),
                    question.correctAnswer(),
                    question.explanation(),
                    question.difficulty(),
                    question.sourceReference()));
        }
    }

    @Transactional
    public List<StudyPackage> findStale(Instant cutoff) {
        return studyPackageRepository.findByStatusInAndUpdatedAtBefore(
                EnumSet.of(StudyPackageStatus.QUEUED, StudyPackageStatus.PROCESSING), cutoff);
    }

    @Transactional
    public void markInterrupted(UUID studyPackId) {
        fail(studyPackId,
                "Generation was interrupted by an application restart. Start a new study package to try again.");
    }

    private String truncate(String value, int max) {
        if (value == null) {
            return "Generation failed.";
        }
        return value.length() > max ? value.substring(0, max) : value;
    }
}
