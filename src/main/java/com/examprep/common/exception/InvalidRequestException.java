package com.examprep.common.exception;

/**
 * A request that cannot be honoured for a reason the student should see, such as starting
 * a study package before extraction has finished.
 */
public class InvalidRequestException extends RuntimeException {

    public InvalidRequestException(String message) {
        super(message);
    }
}
