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
    public boolean isAvailable() {
        return false;
    }

    @Override
    public String putFile(ArtifactPurpose purpose, Path content, StoredMetadata metadata) {
        throw createUnavailableError();
    }

    @Override
    public Optional<StoredMetadata> loadMetadata(String fileId) {
        throw createUnavailableError();
    }

    @Override
    public InputStream open(String fileId) {
        throw createUnavailableError();
    }

    @Override
    public void deleteFile(String fileId) {
        throw createUnavailableError();
    }

    private static StorageUnavailableException createUnavailableError() {
        return new StorageUnavailableException("Chưa cấu hình Google Drive", null);
    }
}
