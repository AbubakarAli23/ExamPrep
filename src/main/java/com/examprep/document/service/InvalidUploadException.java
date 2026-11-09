package com.examprep.document.service;

/**
 * A rejected upload. The message is written for students.
 */
public class InvalidUploadException extends RuntimeException {

    public InvalidUploadException(String message) {
        super(message);
    }
}
