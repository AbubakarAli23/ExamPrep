package com.examprep.support;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Temporary private storage location shared by the test Spring contexts.
 */
public final class StorageSupport {

    private static final Path LOCATION = createDirectory();

    private StorageSupport() {
    }

    private static Path createDirectory() {
        try {
            return Files.createTempDirectory("examprep-test-uploads-");
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    public static String location() {
        return LOCATION.toString();
    }
}
