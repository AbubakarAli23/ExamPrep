package com.examprep.document.storage;

import java.io.IOException;
import java.io.InputStream;
import org.springframework.web.multipart.MultipartFile;

/**
 * Private document storage. Keys are internal identifiers; client filenames are never used
 * as paths.
 */
public interface FileStorage {

    StoredFile store(MultipartFile file, String extension) throws IOException;

    InputStream open(String storageKey) throws IOException;

    void delete(String storageKey);

    boolean exists(String storageKey);

    record StoredFile(String key, long sizeBytes) {
    }
}
