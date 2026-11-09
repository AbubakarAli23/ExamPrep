package com.examprep.web.controller;

import com.examprep.common.exception.InvalidRequestException;
import com.examprep.security.CurrentUserService;
import com.examprep.study.domain.AttemptStatus;
import com.examprep.study.dto.StudyPackDtos.ExamView;
import com.examprep.study.dto.StudyPackDtos.PracticeQuestionView;
import com.examprep.study.dto.StudyPackDtos.QuizResultDto;
import com.examprep.study.dto.StudyPackDtos.QuizSubmissionDto;
import com.examprep.study.dto.StudyPackDtos.StudyPackDto;
import com.examprep.study.quiz.QuizService;
import com.examprep.study.service.StudyPackApplicationService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ExamController {

    private static final String ANSWER_PREFIX = "question-";

    private final QuizService quizService;
    private final StudyPackApplicationService studyPackApplicationService;
    private final CurrentUserService currentUserService;

    public ExamController(QuizService quizService,
                          StudyPackApplicationService studyPackApplicationService,
                          CurrentUserService currentUserService) {
        this.quizService = quizService;
        this.studyPackApplicationService = studyPackApplicationService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/study/{studyPackId}/practice")
    public String practice(@PathVariable UUID studyPackId, Model model) {
        UUID ownerId = currentUserService.requireCurrentUserId();
        StudyPackDto pack = studyPackApplicationService.getStudyPack(studyPackId, ownerId);
        List<PracticeQuestionView> questions = quizService.practiceQuestions(studyPackId, ownerId);
        model.addAttribute("pack", pack);
        model.addAttribute("questions", questions);
        model.addAttribute("activeNav", "documents");
        model.addAttribute("activeSection", "practice");
        model.addAttribute("pageTitle", pack.title());
        return "study/practice";
    }

    @PostMapping("/study/{studyPackId}/mock-exams")
    public String startExam(@PathVariable UUID studyPackId,
                            @RequestParam(name = "durationMinutes", defaultValue = "20") int durationMinutes,
                            RedirectAttributes redirectAttributes) {
        UUID ownerId = currentUserService.requireCurrentUserId();
        try {
            UUID attemptId = quizService.startExam(studyPackId, ownerId, durationMinutes);
            return "redirect:/exams/" + attemptId;
        } catch (InvalidRequestException ex) {
            redirectAttributes.addFlashAttribute("uploadError", ex.getMessage());
            return "redirect:/study/" + studyPackId + "/exam";
        }
    }

    @GetMapping("/exams/{attemptId}")
    public String takeExam(@PathVariable UUID attemptId, Model model) {
        UUID ownerId = currentUserService.requireCurrentUserId();
        ExamView exam = quizService.getExam(attemptId, ownerId);
        if (AttemptStatus.SUBMITTED.name().equals(exam.status())) {
            return "redirect:/exams/" + attemptId + "/results";
        }
        model.addAttribute("exam", exam);
        model.addAttribute("deadlineEpochMillis", exam.startedAtEpochMillis() + exam.allowedSeconds() * 1000L);
        model.addAttribute("activeNav", "documents");
        return "exam/take";
    }

    @PostMapping("/exams/{attemptId}/submit")
    public String submit(@PathVariable UUID attemptId,
                         @RequestParam Map<String, String> params,
                         RedirectAttributes redirectAttributes) {
        UUID ownerId = currentUserService.requireCurrentUserId();
        Map<String, String> answers = new LinkedHashMap<>();
        params.forEach((key, value) -> {
            if (key.startsWith(ANSWER_PREFIX)) {
                answers.put(key.substring(ANSWER_PREFIX.length()), value);
            }
        });
        try {
            quizService.submitAttempt(attemptId, ownerId, new QuizSubmissionDto(answers));
        } catch (InvalidRequestException ex) {
            redirectAttributes.addFlashAttribute("uploadError", ex.getMessage());
            return "redirect:/exams/" + attemptId;
        }
        return "redirect:/exams/" + attemptId + "/results";
    }

    @GetMapping("/exams/{attemptId}/results")
    public String results(@PathVariable UUID attemptId, Model model) {
        UUID ownerId = currentUserService.requireCurrentUserId();
        QuizResultDto result = quizService.getResult(attemptId, ownerId);
        model.addAttribute("result", result);
        model.addAttribute("activeNav", "documents");
        return "exam/results";
    }
}
