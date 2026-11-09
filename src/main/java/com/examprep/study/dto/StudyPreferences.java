package com.examprep.study.dto;

import java.time.LocalDate;

/**
 * Student choices for a generated package. Counts are clamped to safe bounds by the
 * controller and service.
 */
public record StudyPreferences(
        String language,
        String academicLevel,
        LocalDate examDate,
        int practiceQuestionCount,
        int mockQuestionCount,
        int flashcardCount) {

    public static StudyPreferences defaults() {
        return new StudyPreferences("English", "Undergraduate", null, 12, 10, 16);
    }

    public StudyPreferences normalized() {
        String normalizedLanguage = blankToDefault(language, "English");
        String normalizedLevel = blankToDefault(academicLevel, "Undergraduate");
        return new StudyPreferences(
                normalizedLanguage.length() > 40 ? normalizedLanguage.substring(0, 40) : normalizedLanguage,
                normalizedLevel.length() > 60 ? normalizedLevel.substring(0, 60) : normalizedLevel,
                examDate,
                clamp(practiceQuestionCount, 5, 30, 12),
                clamp(mockQuestionCount, 5, 25, 10),
                clamp(flashcardCount, 5, 40, 16));
    }

    private static int clamp(int value, int min, int max, int fallback) {
        if (value <= 0) {
            return fallback;
        }
        return Math.max(min, Math.min(max, value));
    }

    private static String blankToDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
