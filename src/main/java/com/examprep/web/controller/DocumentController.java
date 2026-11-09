package com.examprep.web.controller;

import com.examprep.config.AppProperties;
import com.examprep.common.exception.InvalidRequestException;
import com.examprep.common.exception.ResourceNotFoundException;
import com.examprep.document.dto.DocumentDtos.DocumentStatusDto;
import com.examprep.document.dto.DocumentDtos.DocumentUploadResult;
import com.examprep.document.service.DocumentApplicationService;
import com.examprep.document.service.InvalidUploadException;
import com.examprep.security.CurrentUserService;
import com.examprep.study.dto.StudyPackDtos.StudyPackCreationResult;
import com.examprep.study.service.StudyPackApplicationService;
import com.examprep.web.forms.StudyPackageForm;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/documents")
public class DocumentController {

    private final DocumentApplicationService documentApplicationService;
    private final StudyPackApplicationService studyPackApplicationService;
    private final CurrentUserService currentUserService;
    private final AppProperties appProperties;

    public DocumentController(DocumentApplicationService documentApplicationService,
                              StudyPackApplicationService studyPackApplicationService,
                              CurrentUserService currentUserService,
                              AppProperties appProperties) {
        this.documentApplicationService = documentApplicationService;
        this.studyPackApplicationService = studyPackApplicationService;
        this.currentUserService = currentUserService;
        this.appProperties = appProperties;
    }

    @GetMapping
    public String documents(Model model) {
        UUID ownerId = currentUserService.requireCurrentUserId();
        model.addAttribute("documents", documentApplicationService.listDocuments(ownerId));
        model.addAttribute("maxUploadMb", appProperties.getMaxUploadSizeMb());
        model.addAttribute("activeNav", "documents");
        return "documents/index";
    }

    @PostMapping("/upload")
    public String upload(@RequestParam("file") MultipartFile file, RedirectAttributes redirectAttributes) {
        UUID ownerId = currentUserService.requireCurrentUserId();
        try {
            DocumentUploadResult result = documentApplicationService.upload(file, ownerId);
            redirectAttributes.addFlashAttribute("notice", result.message());
            redirectAttributes.addFlashAttribute("highlightDocumentId", result.documentId());
        } catch (InvalidUploadException ex) {
            redirectAttributes.addFlashAttribute("uploadError", ex.getMessage());
        }
        return "redirect:/documents";
    }

    /**
     * JSON upload used by the drag-and-drop / progress interface.
     */
    @PostMapping("/upload-json")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> uploadJson(@RequestParam("file") MultipartFile file) {
        UUID ownerId = currentUserService.requireCurrentUserId();
        try {
            DocumentUploadResult result = documentApplicationService.upload(file, ownerId);
            return ResponseEntity.ok(Map.of(
                    "documentId", result.documentId(),
                    "filename", result.originalFilename(),
                    "status", result.status(),
                    "message", result.message()));
        } catch (InvalidUploadException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @GetMapping("/{documentId}/status")
    @ResponseBody
    public ResponseEntity<DocumentStatusDto> status(@PathVariable UUID documentId) {
        UUID ownerId = currentUserService.requireCurrentUserId();
        try {
            return ResponseEntity.ok(documentApplicationService.getStatus(documentId, ownerId));
        } catch (ResourceNotFoundException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @PostMapping("/{documentId}/delete")
    public String delete(@PathVariable UUID documentId, RedirectAttributes redirectAttributes) {
        UUID ownerId = currentUserService.requireCurrentUserId();
        try {
            documentApplicationService.delete(documentId, ownerId);
            redirectAttributes.addFlashAttribute("notice", "Document deleted, along with its study packages.");
        } catch (ResourceNotFoundException ex) {
            redirectAttributes.addFlashAttribute("uploadError", "That document could not be found.");
        }
        return "redirect:/documents";
    }

    @PostMapping("/{documentId}/study-packages")
    public String createStudyPackage(@PathVariable UUID documentId,
                                     @ModelAttribute("studyPackageForm") StudyPackageForm form,
                                     RedirectAttributes redirectAttributes) {
        UUID ownerId = currentUserService.requireCurrentUserId();
        try {
            StudyPackCreationResult result = studyPackApplicationService.create(
                    documentId, ownerId, form.toPreferences());
            redirectAttributes.addFlashAttribute("notice", result.message());
            return "redirect:/study/" + result.studyPackId();
        } catch (InvalidRequestException ex) {
            redirectAttributes.addFlashAttribute("uploadError", ex.getMessage());
            return "redirect:/documents";
        } catch (ResourceNotFoundException ex) {
            redirectAttributes.addFlashAttribute("uploadError", "That document could not be found.");
            return "redirect:/documents";
        }
    }
}
