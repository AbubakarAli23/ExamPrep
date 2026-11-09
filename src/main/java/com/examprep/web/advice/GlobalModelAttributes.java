package com.examprep.web.advice;

import java.time.Year;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalModelAttributes {

    @ModelAttribute("currentYear")
    public int currentYear() {
        return Year.now().getValue();
    }

    @ModelAttribute("appName")
    public String appName() {
        return "ExamPrep";
    }
}
