package com.examprep.web.advice;

import com.examprep.ai.exception.AiConfigurationException;
import com.examprep.ai.exception.AiResponseException;
import com.examprep.ai.exception.AiServiceException;
import com.examprep.common.exception.InvalidRequestException;
import com.examprep.common.exception.ResourceNotFoundException;
import com.examprep.document.service.InvalidUploadException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Friendly, safe error pages. Internal details are logged, never rendered.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ModelAndView notFound(ResourceNotFoundException ex, HttpServletRequest request) {
        return error("Not found", ex.getMessage(), HttpStatus.NOT_FOUND, request);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ModelAndView pageNotFound(NoResourceFoundException ex, HttpServletRequest request) {
        return error("Page not found",
                "The page you requested does not exist. Use the navigation to continue studying.",
                HttpStatus.NOT_FOUND, request);
    }

    @ExceptionHandler({InvalidUploadException.class, InvalidRequestException.class})
    public ModelAndView badRequest(RuntimeException ex, HttpServletRequest request) {
        return error("That request could not be completed", ex.getMessage(), HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ModelAndView tooLarge(MaxUploadSizeExceededException ex, HttpServletRequest request) {
        return error("File too large",
                "The upload exceeded the configured size limit. Choose a smaller document.",
                HttpStatus.PAYLOAD_TOO_LARGE, request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ModelAndView forbidden(AccessDeniedException ex, HttpServletRequest request) {
        return error("Access denied",
                "You do not have permission to view this part of ExamPrep.",
                HttpStatus.FORBIDDEN, request);
    }

    @ExceptionHandler(AiConfigurationException.class)
    public ModelAndView aiNotConfigured(AiConfigurationException ex, HttpServletRequest request) {
        return error("AI features are not configured", ex.getMessage(), HttpStatus.SERVICE_UNAVAILABLE, request);
    }

    @ExceptionHandler({AiServiceException.class, AiResponseException.class})
    public ModelAndView aiFailure(RuntimeException ex, HttpServletRequest request) {
        return error("The AI service could not complete that", ex.getMessage(), HttpStatus.BAD_GATEWAY, request);
    }

    @ExceptionHandler(Exception.class)
    public ModelAndView unexpected(Exception ex, HttpServletRequest request) {
        log.error("Unhandled error for {} {}", request.getMethod(), request.getRequestURI(), ex);
        return error("Something went wrong",
                "ExamPrep could not complete that request. Please try again.",
                HttpStatus.INTERNAL_SERVER_ERROR, request);
    }

    private ModelAndView error(String title, String message, HttpStatus status, HttpServletRequest request) {
        ModelAndView view = new ModelAndView("error");
        view.setStatus(status);
        view.addObject("status", status.value());
        view.addObject("title", title);
        view.addObject("message", message == null ? title : message);
        view.addObject("path", request.getRequestURI());
        return view;
    }
}
