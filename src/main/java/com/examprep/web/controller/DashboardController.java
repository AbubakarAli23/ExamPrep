package com.examprep.web.controller;

import com.examprep.document.dto.DocumentDtos.DocumentSummaryDto;
import com.examprep.document.service.DocumentApplicationService;
import com.examprep.security.CurrentUserService;
import com.examprep.security.StudentPrincipal;
import com.examprep.study.dto.StudyPackDtos.StudyProgressDto;
import com.examprep.study.service.StudyPackApplicationService;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    private final CurrentUserService currentUserService;
    private final DocumentApplicationService documentApplicationService;
    private final StudyPackApplicationService studyPackApplicationService;

    public DashboardController(CurrentUserService currentUserService,
                               DocumentApplicationService documentApplicationService,
                               StudyPackApplicationService studyPackApplicationService) {
        this.currentUserService = currentUserService;
        this.documentApplicationService = documentApplicationService;
        this.studyPackApplicationService = studyPackApplicationService;
    }

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal StudentPrincipal principal, Model model) {
        UUID ownerId = currentUserService.requireCurrentUserId();
        List<DocumentSummaryDto> documents = documentApplicationService.listDocuments(ownerId);
        StudyProgressDto progress = studyPackApplicationService.progress(ownerId);

        model.addAttribute("firstName", firstName(principal));
        model.addAttribute("documents", documents.stream().limit(4).toList());
        model.addAttribute("documentCount", documents.size());
        model.addAttribute("studyPackages", studyPackApplicationService.listRecent(ownerId));
        model.addAttribute("attempts", studyPackApplicationService.recentAttempts(ownerId));
        model.addAttribute("progress", progress);
        model.addAttribute("activeNav", "dashboard");
        return "dashboard/index";
    }

    private String firstName(StudentPrincipal principal) {
        if (principal == null || principal.getFullName() == null || principal.getFullName().isBlank()) {
            return "there";
        }
        return principal.getFullName().trim().split("\\s+")[0];
    }
}
