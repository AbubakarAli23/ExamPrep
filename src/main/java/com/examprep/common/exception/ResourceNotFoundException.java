package com.examprep.common.exception;

/**
 * Thrown when a resource does not exist or is not owned by the current student. Both
 * cases produce the same response so that identifiers cannot be probed.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
