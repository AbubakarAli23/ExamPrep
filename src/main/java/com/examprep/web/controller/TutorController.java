package com.examprep.web.controller;

import com.examprep.ai.config.AiProperties;
import com.examprep.ai.exception.AiConfigurationException;
import com.examprep.ai.exception.AiResponseException;
import com.examprep.ai.exception.AiServiceException;
import com.examprep.common.exception.InvalidRequestException;
import com.examprep.security.CurrentUserService;
import com.examprep.study.dto.StudyPackDtos.StudyPackDto;
import com.examprep.study.dto.StudyPackDtos.TutorMessageView;
import com.examprep.study.dto.StudyPackDtos.TutorQuestionDto;
import com.examprep.study.service.StudyPackApplicationService;
import com.examprep.study.tutor.TutorService;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class TutorController {

    private final TutorService tutorService;
    private final StudyPackApplicationService studyPackApplicationService;
    private final CurrentUserService currentUserService;
    private final AiProperties aiProperties;

    public TutorController(TutorService tutorService,
                           StudyPackApplicationService studyPackApplicationService,
                           CurrentUserService currentUserService,
                           AiProperties aiProperties) {
        this.tutorService = tutorService;
        this.studyPackApplicationService = studyPackApplicationService;
        this.currentUserService = currentUserService;
        this.aiProperties = aiProperties;
    }

    @GetMapping("/study/{studyPackId}/tutor")
    public String tutor(@PathVariable UUID studyPackId, Model model) {
        UUID ownerId = currentUserService.requireCurrentUserId();
        StudyPackDto pack = studyPackApplicationService.getStudyPack(studyPackId, ownerId);
        List<TutorMessageView> messages = tutorService.history(studyPackId, ownerId);
        model.addAttribute("pack", pack);
        model.addAttribute("messages", messages);
        model.addAttribute("aiConfigured", aiProperties.isConfigured());
        model.addAttribute("activeNav", "documents");
        model.addAttribute("activeSection", "tutor");
        model.addAttribute("pageTitle", pack.title());
        return "study/tutor";
    }

    @PostMapping("/study/{studyPackId}/tutor")
    public String ask(@PathVariable UUID studyPackId,
                      @RequestParam(name = "question", required = false) String question,
                      RedirectAttributes redirectAttributes) {
        UUID ownerId = currentUserService.requireCurrentUserId();
        try {
            studyPackApplicationService.askTutor(studyPackId, ownerId, new TutorQuestionDto(question));
        } catch (AiConfigurationException ex) {
            redirectAttributes.addFlashAttribute("tutorError", ex.getMessage());
        } catch (AiServiceException | AiResponseException ex) {
            redirectAttributes.addFlashAttribute("tutorError", ex.getMessage());
        } catch (InvalidRequestException ex) {
            redirectAttributes.addFlashAttribute("tutorError", ex.getMessage());
        }
        return "redirect:/study/" + studyPackId + "/tutor";
    }
}
