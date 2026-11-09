package com.examprep.web.controller;

import com.examprep.security.CurrentUserService;
import com.examprep.study.domain.StudyPackageStatus;
import com.examprep.study.dto.StudyPackDtos.StudyPackDto;
import com.examprep.study.dto.StudyPackDtos.StudyPackStatusDto;
import com.examprep.study.quiz.QuizService;
import com.examprep.study.service.StudyPackApplicationService;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class StudyPackController {

    private final StudyPackApplicationService studyPackApplicationService;
    private final QuizService quizService;
    private final CurrentUserService currentUserService;

    public StudyPackController(StudyPackApplicationService studyPackApplicationService,
                               QuizService quizService,
                               CurrentUserService currentUserService) {
        this.studyPackApplicationService = studyPackApplicationService;
        this.quizService = quizService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/study/{studyPackId}")
    public String overview(@PathVariable UUID studyPackId, Model model) {
        StudyPackDto pack = requirePack(studyPackId);
        model.addAttribute("pack", pack);
        model.addAttribute("activeNav", "documents");
        model.addAttribute("activeSection", "overview");
        model.addAttribute("pageTitle", pack.title());
        return "study/overview";
    }

    @GetMapping("/study/{studyPackId}/topics")
    public String topics(@PathVariable UUID studyPackId, Model model) {
        return section(studyPackId, model, "topics", "study/topics");
    }

    @GetMapping("/study/{studyPackId}/notes")
    public String notes(@PathVariable UUID studyPackId, Model model) {
        return section(studyPackId, model, "notes", "study/notes");
    }

    @GetMapping("/study/{studyPackId}/definitions")
    public String definitions(@PathVariable UUID studyPackId, Model model) {
        return section(studyPackId, model, "definitions", "study/definitions");
    }

    @GetMapping("/study/{studyPackId}/formulas")
    public String formulas(@PathVariable UUID studyPackId, Model model) {
        return section(studyPackId, model, "formulas", "study/formulas");
    }

    @GetMapping("/study/{studyPackId}/exam")
    public String examSetup(@PathVariable UUID studyPackId, Model model) {
        StudyPackDto pack = requirePack(studyPackId);
        model.addAttribute("pack", pack);
        model.addAttribute("attempts", quizService.attemptsForPackage(studyPackId, currentUserService.requireCurrentUserId()));
        model.addAttribute("activeNav", "documents");
        model.addAttribute("activeSection", "exam");
        model.addAttribute("pageTitle", pack.title());
        return "study/exam";
    }

    @GetMapping("/study/{studyPackId}/status")
    @ResponseBody
    public ResponseEntity<StudyPackStatusDto> status(@PathVariable UUID studyPackId) {
        UUID ownerId = currentUserService.requireCurrentUserId();
        try {
            return ResponseEntity.ok(studyPackApplicationService.getStatus(studyPackId, ownerId));
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @PostMapping("/study/{studyPackId}/delete")
    public String delete(@PathVariable UUID studyPackId, RedirectAttributes redirectAttributes) {
        UUID ownerId = currentUserService.requireCurrentUserId();
        studyPackApplicationService.delete(studyPackId, ownerId);
        redirectAttributes.addFlashAttribute("notice", "Study package deleted.");
        return "redirect:/documents";
    }

    private String section(UUID studyPackId, Model model, String section, String template) {
        StudyPackDto pack = requirePack(studyPackId);
        if (pack.status().equals(StudyPackageStatus.FAILED.name())
                || pack.status().equals(StudyPackageStatus.QUEUED.name())
                || pack.status().equals(StudyPackageStatus.PROCESSING.name())) {
            return "redirect:/study/" + studyPackId;
        }
        model.addAttribute("pack", pack);
        model.addAttribute("activeNav", "documents");
        model.addAttribute("activeSection", section);
        model.addAttribute("pageTitle", pack.title());
        return template;
    }

    private StudyPackDto requirePack(UUID studyPackId) {
        return studyPackApplicationService.getStudyPack(
                studyPackId, currentUserService.requireCurrentUserId());
    }
}
