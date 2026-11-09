package com.examprep.web.forms;

import com.examprep.study.dto.StudyPreferences;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

public class StudyPackageForm {

    private String language = "English";
    private String academicLevel = "Undergraduate";

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate examDate;

    private int practiceQuestionCount = 12;
    private int mockQuestionCount = 10;
    private int flashcardCount = 16;

    public StudyPreferences toPreferences() {
        return new StudyPreferences(
                language,
                academicLevel,
                examDate,
                practiceQuestionCount,
                mockQuestionCount,
                flashcardCount);
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getAcademicLevel() {
        return academicLevel;
    }

    public void setAcademicLevel(String academicLevel) {
        this.academicLevel = academicLevel;
    }

    public LocalDate getExamDate() {
        return examDate;
    }

    public void setExamDate(LocalDate examDate) {
        this.examDate = examDate;
    }

    public int getPracticeQuestionCount() {
        return practiceQuestionCount;
    }

    public void setPracticeQuestionCount(int practiceQuestionCount) {
        this.practiceQuestionCount = practiceQuestionCount;
    }

    public int getMockQuestionCount() {
        return mockQuestionCount;
    }

    public void setMockQuestionCount(int mockQuestionCount) {
        this.mockQuestionCount = mockQuestionCount;
    }

    public int getFlashcardCount() {
        return flashcardCount;
    }

    public void setFlashcardCount(int flashcardCount) {
        this.flashcardCount = flashcardCount;
    }
}
