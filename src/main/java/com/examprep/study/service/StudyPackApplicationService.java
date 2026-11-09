package com.examprep.study.service;

import com.examprep.study.dto.StudyPackDtos.AttemptSummaryDto;
import com.examprep.study.dto.StudyPackDtos.QuizResultDto;
import com.examprep.study.dto.StudyPackDtos.QuizSubmissionDto;
import com.examprep.study.dto.StudyPackDtos.StudyPackCreationResult;
import com.examprep.study.dto.StudyPackDtos.StudyPackDto;
import com.examprep.study.dto.StudyPackDtos.StudyPackStatusDto;
import com.examprep.study.dto.StudyPackDtos.StudyPackSummaryDto;
import com.examprep.study.dto.StudyPackDtos.StudyProgressDto;
import com.examprep.study.dto.StudyPackDtos.TutorAnswerDto;
import com.examprep.study.dto.StudyPackDtos.TutorQuestionDto;
import com.examprep.study.dto.StudyPreferences;
import java.util.List;
import java.util.UUID;

/**
 * Study module contract used by the web layer.
 */
public interface StudyPackApplicationService {

    StudyPackCreationResult create(UUID documentId, UUID ownerId, StudyPreferences preferences);

    StudyPackStatusDto getStatus(UUID studyPackId, UUID ownerId);

    StudyPackDto getStudyPack(UUID studyPackId, UUID ownerId);

    List<StudyPackSummaryDto> listRecent(UUID ownerId);

    List<AttemptSummaryDto> recentAttempts(UUID ownerId);

    StudyProgressDto progress(UUID ownerId);

    QuizResultDto submitQuiz(UUID attemptId, UUID ownerId, QuizSubmissionDto submission);

    TutorAnswerDto askTutor(UUID studyPackId, UUID ownerId, TutorQuestionDto question);

    void delete(UUID studyPackId, UUID ownerId);
}
