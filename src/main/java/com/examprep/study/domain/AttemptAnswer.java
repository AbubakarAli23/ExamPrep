package com.examprep.study.domain;

import java.util.UUID;

/**
 * One submitted answer, stored with the attempt so results can be reviewed later without
 * recomputation.
 */
public record AttemptAnswer(
        UUID questionId,
        String submittedAnswer,
        Integer selectedOptionIndex,
        Boolean correct) {
}
