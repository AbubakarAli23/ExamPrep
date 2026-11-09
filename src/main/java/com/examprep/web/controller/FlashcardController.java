package com.examprep.web.controller;

import com.examprep.security.CurrentUserService;
import com.examprep.study.dto.StudyPackDtos.FlashcardView;
import com.examprep.study.dto.StudyPackDtos.StudyPackDto;
import com.examprep.study.flashcard.FlashcardService;
import com.examprep.study.service.StudyPackApplicationService;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class FlashcardController {

    private final FlashcardService flashcardService;
    private final StudyPackApplicationService studyPackApplicationService;
    private final CurrentUserService currentUserService;

    public FlashcardController(FlashcardService flashcardService,
                               StudyPackApplicationService studyPackApplicationService,
                               CurrentUserService currentUserService) {
        this.flashcardService = flashcardService;
        this.studyPackApplicationService = studyPackApplicationService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/study/{studyPackId}/flashcards")
    public String flashcards(@PathVariable UUID studyPackId, Model model) {
        UUID ownerId = currentUserService.requireCurrentUserId();
        StudyPackDto pack = studyPackApplicationService.getStudyPack(studyPackId, ownerId);
        List<FlashcardView> cards = flashcardService.flashcards(studyPackId, ownerId);
        model.addAttribute("pack", pack);
        model.addAttribute("cards", cards);
        model.addAttribute("activeNav", "documents");
        model.addAttribute("activeSection", "flashcards");
        model.addAttribute("pageTitle", pack.title());
        return "study/flashcards";
    }

    @PostMapping("/study/{studyPackId}/flashcards/review")
    public String recordReview(@PathVariable UUID studyPackId,
                               @RequestParam(name = "count", defaultValue = "1") int count,
                               RedirectAttributes redirectAttributes) {
        UUID ownerId = currentUserService.requireCurrentUserId();
        flashcardService.recordReview(studyPackId, ownerId, count);
        redirectAttributes.addFlashAttribute("notice", "Flashcard review saved.");
        return "redirect:/study/" + studyPackId + "/flashcards";
    }

    @PostMapping("/study/{studyPackId}/flashcards/review-json")
    @ResponseBody
    public String recordReviewJson(@PathVariable UUID studyPackId,
                                   @RequestParam(name = "count", defaultValue = "1") int count) {
        UUID ownerId = currentUserService.requireCurrentUserId();
        flashcardService.recordReview(studyPackId, ownerId, count);
        return "{\"saved\":true}";
    }
}
