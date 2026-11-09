package com.examprep.study.quiz;

import com.examprep.common.exception.InvalidRequestException;
import com.examprep.common.exception.ResourceNotFoundException;
import com.examprep.common.web.DisplayDates;
import com.examprep.study.domain.AttemptAnswer;
import com.examprep.study.domain.AttemptStatus;
import com.examprep.study.domain.QuestionSection;
import com.examprep.study.domain.QuizAttempt;
import com.examprep.study.domain.QuizQuestion;
import com.examprep.study.domain.StudyPackage;
import com.examprep.study.domain.StudyPackageStatus;
import com.examprep.study.dto.StudyPackDtos.AttemptSummaryDto;
import com.examprep.study.dto.StudyPackDtos.ExamQuestionView;
import com.examprep.study.dto.StudyPackDtos.ExamView;
import com.examprep.study.dto.StudyPackDtos.PracticeQuestionView;
import com.examprep.study.dto.StudyPackDtos.QuestionResult;
import com.examprep.study.dto.StudyPackDtos.QuizResultDto;
import com.examprep.study.dto.StudyPackDtos.QuizSubmissionDto;
import com.examprep.study.repository.QuizAttemptRepository;
import com.examprep.study.repository.QuizQuestionRepository;
import com.examprep.study.repository.StudyPackageRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Examination flow: an attempt is created up front, answer keys stay on the server, and
 * scoring happens only when the student submits.
 */
@Service
public class QuizService {

    private static final int MAX_EXAM_SECONDS = 4 * 60 * 60;

    private final StudyPackageRepository studyPackageRepository;
    private final QuizQuestionRepository quizQuestionRepository;
    private final QuizAttemptRepository quizAttemptRepository;

    public QuizService(StudyPackageRepository studyPackageRepository,
                       QuizQuestionRepository quizQuestionRepository,
                       QuizAttemptRepository quizAttemptRepository) {
        this.studyPackageRepository = studyPackageRepository;
        this.quizQuestionRepository = quizQuestionRepository;
        this.quizAttemptRepository = quizAttemptRepository;
    }

    @Transactional
    public UUID startExam(UUID studyPackId, UUID ownerId, int durationMinutes) {
        StudyPackage studyPackage = requirePackage(studyPackId, ownerId);
        if (studyPackage.getStatus() != StudyPackageStatus.READY) {
            throw new InvalidRequestException("This study package is still being generated.");
        }
        List<QuizQuestion> questions = quizQuestionRepository
                .findByStudyPackageIdAndSectionOrderByPositionAsc(studyPackId, QuestionSection.MOCK);
        QuestionSection section = QuestionSection.MOCK;
        if (questions.isEmpty()) {
            questions = quizQuestionRepository
                    .findByStudyPackageIdAndSectionOrderByPositionAsc(studyPackId, QuestionSection.PRACTICE);
            section = QuestionSection.PRACTICE;
        }
        if (questions.isEmpty()) {
            throw new InvalidRequestException("This study package does not have examination questions yet.");
        }

        int minutes = Math.max(5, Math.min(120, durationMinutes <= 0 ? 20 : durationMinutes));
        int gradable = (int) questions.stream()
                .filter(question -> question.getType() != com.examprep.study.domain.content.StudyContent.QuestionType.SHORT_ANSWER)
                .count();

        QuizAttempt attempt = new QuizAttempt(
                UUID.randomUUID(), ownerId, studyPackId, section,
                questions.size(), gradable, minutes * 60, Instant.now());
        quizAttemptRepository.save(attempt);
        return attempt.getId();
    }

    @Transactional(readOnly = true)
    public List<PracticeQuestionView> practiceQuestions(UUID studyPackId, UUID ownerId) {
        StudyPackage studyPackage = requirePackage(studyPackId, ownerId);
        if (studyPackage.getStatus() != StudyPackageStatus.READY) {
            throw new InvalidRequestException("This study package is still being generated.");
        }
        return quizQuestionRepository
                .findByStudyPackageIdAndSectionOrderByPositionAsc(studyPackId, QuestionSection.PRACTICE)
                .stream()
                .map(question -> new PracticeQuestionView(
                        question.getId(),
                        question.getType().name(),
                        question.getPrompt(),
                        question.getOptions() == null ? List.of() : question.getOptions(),
                        question.getCorrectAnswer(),
                        question.getExplanation(),
                        question.getDifficulty().name(),
                        question.getSourceReference()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AttemptSummaryDto> attemptsForPackage(UUID studyPackId, UUID ownerId) {
        requirePackage(studyPackId, ownerId);
        List<AttemptSummaryDto> summaries = new ArrayList<>();
        for (QuizAttempt attempt : quizAttemptRepository.findByStudyPackageIdAndOwnerId(studyPackId, ownerId)) {
            if (attempt.getStatus() != AttemptStatus.SUBMITTED) {
                continue;
            }
            summaries.add(new AttemptSummaryDto(
                    attempt.getId(),
                    attempt.getStudyPackageId(),
                    "",
                    attempt.getScorePercent() == null ? 0 : attempt.getScorePercent(),
                    attempt.getCorrectCount() == null ? 0 : attempt.getCorrectCount(),
                    attempt.getGradableQuestions(),
                    DisplayDates.dateTime(attempt.getSubmittedAt()),
                    DisplayDates.duration(attempt.getDurationSeconds() == null ? 0 : attempt.getDurationSeconds())));
        }
        return summaries;
    }

    @Transactional(readOnly = true)
    public ExamView getExam(UUID attemptId, UUID ownerId) {
        QuizAttempt attempt = requireAttempt(attemptId, ownerId);
        StudyPackage studyPackage = requirePackage(attempt.getStudyPackageId(), ownerId);
        List<ExamQuestionView> questions = questionsFor(attempt).stream()
                .map(question -> new ExamQuestionView(
                        question.getId(),
                        question.getPosition(),
                        question.getType().name(),
                        question.getPrompt(),
                        question.getOptions() == null ? List.of() : question.getOptions(),
                        question.getDifficulty().name(),
                        question.getSourceReference()))
                .toList();
        return new ExamView(
                attempt.getId(),
                attempt.getStudyPackageId(),
                studyPackage.getTitle() == null ? "Mock examination" : studyPackage.getTitle(),
                attempt.getStatus().name(),
                attempt.getAllowedSeconds(),
                attempt.getTotalQuestions(),
                attempt.getStartedAt().toEpochMilli(),
                DisplayDates.dateTime(attempt.getStartedAt()),
                questions);
    }

    @Transactional
    public QuizResultDto submitAttempt(UUID attemptId, UUID ownerId, QuizSubmissionDto submission) {
        QuizAttempt attempt = requireAttempt(attemptId, ownerId);
        if (attempt.getStatus() == AttemptStatus.SUBMITTED) {
            // Idempotent: a double submit (or a timed-out browser resubmit) cannot resubmit
            // or alter a marked paper.
            return buildResult(attempt, ownerId);
        }

        List<QuizQuestion> questions = questionsFor(attempt);
        Map<String, String> answers = submission == null || submission.answers() == null
                ? Map.of()
                : submission.answers();

        List<AttemptAnswer> storedAnswers = new ArrayList<>();
        int correctCount = 0;
        int gradable = 0;
        for (QuizQuestion question : questions) {
            String raw = answers.get(question.getId().toString());
            String submittedAnswer = null;
            Integer selectedIndex = null;
            Boolean correct = null;

            if (question.getType() == com.examprep.study.domain.content.StudyContent.QuestionType.SHORT_ANSWER) {
                submittedAnswer = raw == null || raw.isBlank() ? null : raw.strip();
            } else {
                gradable++;
                selectedIndex = parseIndex(raw);
                List<String> options = question.getOptions() == null ? List.of() : question.getOptions();
                if (selectedIndex != null && selectedIndex >= 0 && selectedIndex < options.size()) {
                    submittedAnswer = options.get(selectedIndex);
                } else if (raw != null && !raw.isBlank()) {
                    submittedAnswer = raw.strip();
                    selectedIndex = null;
                }
                correct = submittedAnswer != null
                        && submittedAnswer.strip().equalsIgnoreCase(question.getCorrectAnswer().strip());
                if (Boolean.TRUE.equals(correct)) {
                    correctCount++;
                }
            }
            storedAnswers.add(new AttemptAnswer(question.getId(), submittedAnswer, selectedIndex, correct));
        }

        int scorePercent = gradable == 0 ? 0 : (int) Math.round(100.0 * correctCount / gradable);
        Instant submittedAt = Instant.now();
        int elapsed = (int) Math.max(0, Math.min(MAX_EXAM_SECONDS,
                Duration.between(attempt.getStartedAt(), submittedAt).getSeconds()));

        attempt.setAnswers(storedAnswers);
        attempt.setCorrectCount(correctCount);
        attempt.setScorePercent(scorePercent);
        attempt.setDurationSeconds(elapsed);
        attempt.setSubmittedAt(submittedAt);
        attempt.setStatus(AttemptStatus.SUBMITTED);
        quizAttemptRepository.save(attempt);

        return buildResult(attempt, ownerId);
    }

    @Transactional(readOnly = true)
    public QuizResultDto getResult(UUID attemptId, UUID ownerId) {
        QuizAttempt attempt = requireAttempt(attemptId, ownerId);
        if (attempt.getStatus() != AttemptStatus.SUBMITTED) {
            throw new InvalidRequestException("This examination has not been submitted yet.");
        }
        return buildResult(attempt, ownerId);
    }

    @Transactional(readOnly = true)
    public List<AttemptSummaryDto> recentAttempts(UUID ownerId) {
        List<QuizAttempt> attempts = quizAttemptRepository
                .findTop5ByOwnerIdAndStatusOrderBySubmittedAtDesc(ownerId, AttemptStatus.SUBMITTED);
        List<AttemptSummaryDto> summaries = new ArrayList<>();
        for (QuizAttempt attempt : attempts) {
            String title = studyPackageRepository.findById(attempt.getStudyPackageId())
                    .map(StudyPackage::getTitle)
                    .orElse("Study package");
            summaries.add(new AttemptSummaryDto(
                    attempt.getId(),
                    attempt.getStudyPackageId(),
                    title,
                    attempt.getScorePercent() == null ? 0 : attempt.getScorePercent(),
                    attempt.getCorrectCount() == null ? 0 : attempt.getCorrectCount(),
                    attempt.getGradableQuestions(),
                    DisplayDates.dateTime(attempt.getSubmittedAt()),
                    DisplayDates.duration(attempt.getDurationSeconds() == null ? 0 : attempt.getDurationSeconds())));
        }
        return summaries;
    }

    @Transactional(readOnly = true)
    public ScoreStats scoreStats(UUID ownerId) {
        List<QuizAttempt> attempts = quizAttemptRepository
                .findByOwnerIdAndStatusOrderBySubmittedAtDesc(ownerId, AttemptStatus.SUBMITTED);
        if (attempts.isEmpty()) {
            return new ScoreStats(0, null, null);
        }
        int total = 0;
        int best = 0;
        for (QuizAttempt attempt : attempts) {
            int score = attempt.getScorePercent() == null ? 0 : attempt.getScorePercent();
            total += score;
            best = Math.max(best, score);
        }
        return new ScoreStats(attempts.size(), (int) Math.round((double) total / attempts.size()), best);
    }

    private List<QuizQuestion> questionsFor(QuizAttempt attempt) {
        return quizQuestionRepository.findByStudyPackageIdAndSectionOrderByPositionAsc(
                attempt.getStudyPackageId(), attempt.getQuestionSection());
    }

    private Integer parseIndex(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private QuizResultDto buildResult(QuizAttempt attempt, UUID ownerId) {
        StudyPackage studyPackage = requirePackage(attempt.getStudyPackageId(), ownerId);
        Map<UUID, AttemptAnswer> answersById = new LinkedHashMapBuilder(attempt.getAnswers()).map();
        List<QuestionResult> results = new ArrayList<>();
        Set<String> revisionAreas = new LinkedHashSet<>();

        List<QuizQuestion> questions = new ArrayList<>(questionsFor(attempt));
        questions.sort(Comparator.comparingInt(QuizQuestion::getPosition));

        for (QuizQuestion question : questions) {
            AttemptAnswer answer = answersById.get(question.getId());
            results.add(new QuestionResult(
                    question.getId(),
                    question.getPosition(),
                    question.getType().name(),
                    question.getPrompt(),
                    question.getOptions() == null ? List.of() : question.getOptions(),
                    answer == null ? null : answer.submittedAnswer(),
                    question.getCorrectAnswer(),
                    answer == null ? null : answer.correct(),
                    question.getExplanation(),
                    question.getDifficulty().name(),
                    question.getSourceReference()));
            if (answer != null && Boolean.FALSE.equals(answer.correct()) && question.getSourceReference() != null) {
                revisionAreas.add(question.getSourceReference());
            }
        }

        int score = attempt.getScorePercent() == null ? 0 : attempt.getScorePercent();
        int duration = attempt.getDurationSeconds() == null ? 0 : attempt.getDurationSeconds();
        return new QuizResultDto(
                attempt.getId(),
                attempt.getStudyPackageId(),
                studyPackage.getTitle() == null ? "Mock examination" : studyPackage.getTitle(),
                score,
                attempt.getCorrectCount() == null ? 0 : attempt.getCorrectCount(),
                attempt.getGradableQuestions(),
                attempt.getTotalQuestions(),
                duration,
                DisplayDates.duration(duration),
                DisplayDates.dateTime(attempt.getSubmittedAt()),
                headline(score),
                new ArrayList<>(revisionAreas),
                results);
    }

    private String headline(int score) {
        if (score >= 85) {
            return "Strong result. Keep consolidating the details.";
        }
        if (score >= 60) {
            return "A solid attempt. Review the explanations below before retrying.";
        }
        return "This needs another pass. Use the revision notes, then try again.";
    }

    private QuizAttempt requireAttempt(UUID attemptId, UUID ownerId) {
        return quizAttemptRepository.findByIdAndOwnerId(attemptId, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("That examination attempt could not be found."));
    }

    private StudyPackage requirePackage(UUID studyPackId, UUID ownerId) {
        return studyPackageRepository.findByIdAndOwnerId(studyPackId, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("That study package could not be found."));
    }

    public record ScoreStats(int completedExams, Integer averageScorePercent, Integer bestScorePercent) {
    }

    private static final class LinkedHashMapBuilder {
        private final java.util.Map<UUID, AttemptAnswer> map = new java.util.LinkedHashMap<>();

        LinkedHashMapBuilder(List<AttemptAnswer> answers) {
            if (answers != null) {
                for (AttemptAnswer answer : answers) {
                    map.put(answer.questionId(), answer);
                }
            }
        }

        java.util.Map<UUID, AttemptAnswer> map() {
            return map;
        }
    }
}
