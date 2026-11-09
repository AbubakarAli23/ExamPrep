package com.examprep.study.service;

import com.examprep.common.web.DisplayDates;
import com.examprep.study.domain.StudyPackage;
import com.examprep.study.domain.StudyPackageStatus;
import com.examprep.study.dto.StudyPackDtos.StudyPackDto;
import com.examprep.study.dto.StudyPackDtos.StudyPackStatusDto;
import com.examprep.study.dto.StudyPackDtos.StudyPackSummaryDto;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class StudyPackageMapper {

    public StudyPackStatusDto toStatus(StudyPackage studyPackage) {
        return new StudyPackStatusDto(
                studyPackage.getId(),
                studyPackage.getDocumentId(),
                studyPackage.getStatus().name(),
                progress(studyPackage),
                message(studyPackage),
                studyPackage.getFailureReason(),
                studyPackage.getStatus() == StudyPackageStatus.READY);
    }

    public StudyPackDto toDto(StudyPackage studyPackage,
                              String documentTitle,
                              long flashcardCount,
                              long practiceQuestionCount,
                              long mockQuestionCount) {
        return new StudyPackDto(
                studyPackage.getId(),
                studyPackage.getDocumentId(),
                documentTitle,
                studyPackage.getTitle() == null ? "Study package" : studyPackage.getTitle(),
                studyPackage.getStatus().name(),
                studyPackage.getIntroduction(),
                studyPackage.getOverallSummary(),
                studyPackage.getLearningObjectives() == null ? List.of() : studyPackage.getLearningObjectives(),
                studyPackage.getRevisionPriorities() == null ? List.of() : studyPackage.getRevisionPriorities(),
                studyPackage.getTopics() == null ? List.of() : studyPackage.getTopics(),
                studyPackage.getRevisionNotes() == null ? List.of() : studyPackage.getRevisionNotes(),
                studyPackage.getDefinitions() == null ? List.of() : studyPackage.getDefinitions(),
                studyPackage.getFormulas() == null ? List.of() : studyPackage.getFormulas(),
                flashcardCount,
                practiceQuestionCount,
                mockQuestionCount,
                studyPackage.getLanguage(),
                studyPackage.getAcademicLevel(),
                DisplayDates.date(studyPackage.getExamDate()),
                DisplayDates.dateTime(studyPackage.getCreatedAt()),
                studyPackage.getFailureReason(),
                studyPackage.getProgressMessage(),
                studyPackage.getStatus() == StudyPackageStatus.READY,
                studyPackage.getAiModel());
    }

    public StudyPackSummaryDto toSummary(StudyPackage studyPackage,
                                         String documentTitle,
                                         long flashcardCount,
                                         long practiceQuestionCount,
                                         long mockQuestionCount) {
        return new StudyPackSummaryDto(
                studyPackage.getId(),
                studyPackage.getDocumentId(),
                studyPackage.getTitle() == null ? "Study package" : studyPackage.getTitle(),
                documentTitle,
                studyPackage.getStatus().name(),
                DisplayDates.dateTime(studyPackage.getCreatedAt()),
                flashcardCount,
                practiceQuestionCount,
                mockQuestionCount,
                studyPackage.getStatus() == StudyPackageStatus.READY);
    }

    public int progress(StudyPackage studyPackage) {
        return switch (studyPackage.getStatus()) {
            case QUEUED -> 10;
            case PROCESSING -> 55;
            case READY -> 100;
            case FAILED -> 100;
        };
    }

    private String message(StudyPackage studyPackage) {
        if (studyPackage.getStatus() == StudyPackageStatus.FAILED) {
            return studyPackage.getFailureReason() == null
                    ? "This study package could not be generated."
                    : studyPackage.getFailureReason();
        }
        if (studyPackage.getStatus() == StudyPackageStatus.READY) {
            return "Your study package is ready.";
        }
        return studyPackage.getProgressMessage() == null ? "Working on your study package." : studyPackage.getProgressMessage();
    }
}
