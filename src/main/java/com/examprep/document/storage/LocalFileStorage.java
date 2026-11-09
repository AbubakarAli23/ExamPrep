package com.examprep.document.storage;

import com.examprep.config.AppProperties;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * Stores uploads on the local filesystem, outside any publicly served directory, using
 * generated keys only.
 */
@Component
public class LocalFileStorage implements FileStorage {

    private static final Logger log = LoggerFactory.getLogger(LocalFileStorage.class);

    private final AppProperties appProperties;
    private Path root;

    public LocalFileStorage(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @PostConstruct
    void initialize() throws IOException {
        root = Path.of(appProperties.getStorage().getLocation()).toAbsolutePath().normalize();
        Files.createDirectories(root);
        log.info("Document storage initialised at {}", root);
    }

    @Override
    public StoredFile store(MultipartFile file, String extension) throws IOException {
        String key = UUID.randomUUID() + (extension.isBlank() ? "" : "." + extension);
        Path target = resolve(key);
        Files.createDirectories(root);
        try (InputStream input = file.getInputStream()) {
            Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
        }
        return new StoredFile(key, Files.size(target));
    }

    @Override
    public InputStream open(String storageKey) throws IOException {
        return Files.newInputStream(resolve(storageKey));
    }

    @Override
    public void delete(String storageKey) {
        try {
            Files.deleteIfExists(resolve(storageKey));
        } catch (IOException ex) {
            log.warn("Could not delete stored document {}", storageKey, ex);
        }
    }

    @Override
    public boolean exists(String storageKey) {
        return storageKey != null && Files.exists(resolve(storageKey));
    }

    public Path resolve(String storageKey) {
        if (storageKey == null || storageKey.isBlank() || storageKey.contains("..")) {
            throw new IllegalArgumentException("Invalid storage key");
        }
        Path candidate = root.resolve(storageKey).normalize();
        if (!candidate.startsWith(root)) {
            throw new IllegalArgumentException("Invalid storage key");
        }
        return candidate;
    }
}
