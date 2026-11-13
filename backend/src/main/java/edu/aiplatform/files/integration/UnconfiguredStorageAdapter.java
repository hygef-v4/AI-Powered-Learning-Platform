package edu.aiplatform.files.integration;

import edu.aiplatform.files.service.IStorage;
import edu.aiplatform.files.service.StorageUnavailableException;
import edu.aiplatform.files.service.StoredMetadata;
import edu.aiplatform.files.contract.ArtifactPurpose;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Optional;

/** Production thiếu cấu hình Drive: mọi thao tác báo tạm thời không khả dụng, không lưu vào container. */
public class UnconfiguredStorageAdapter implements IStorage {

    @Override
    public boolean available() {
        return false;
    }

    @Override
    public String put(ArtifactPurpose purpose, Path content, StoredMetadata metadata) {
        throw unavailable();
    }

    @Override
    public Optional<StoredMetadata> metadata(String fileId) {
        throw unavailable();
    }

    @Override
    public InputStream open(String fileId) {
        throw unavailable();
    }

    @Override
    public void delete(String fileId) {
        throw unavailable();
    }

    private static StorageUnavailableException unavailable() {
        return new StorageUnavailableException("Chưa cấu hình Google Drive", null);
    }
}
