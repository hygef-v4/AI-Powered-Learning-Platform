package edu.aiplatform.files.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import edu.aiplatform.files.service.IStorage;
import edu.aiplatform.files.service.StorageUnavailableException;
import edu.aiplatform.files.service.StoredMetadata;
import edu.aiplatform.files.contract.ArtifactPurpose;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

/** Lưu tệp vào thư mục local (chạy local và test, NFR-U03-31); thuộc tính để ở tệp JSON cạnh tệp. */
public class LocalFolderStorageAdapter implements IStorage {

    private static final Pattern FILE_ID = Pattern.compile("local-[0-9a-f-]{36}");
    private static final ObjectMapper JSON = JsonMapper.builder().findAndAddModules().build();

    private final Path root;

    public LocalFolderStorageAdapter(Path root) {
        this.root = root;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String putFile(ArtifactPurpose purpose, Path content, StoredMetadata metadata) {
        String fileId = "local-" + UUID.randomUUID();
        try {
            Files.createDirectories(root);
            Files.copy(content, resolveDataPath(fileId), StandardCopyOption.REPLACE_EXISTING);
            JSON.writeValue(resolveMetaPath(fileId).toFile(), metadata);
            return fileId;
        } catch (IOException e) {
            deleteFile(fileId);
            throw new StorageUnavailableException("Không ghi được tệp local", e);
        }
    }

    @Override
    public Optional<StoredMetadata> loadMetadata(String fileId) {
        if (!isValidFileId(fileId) || !Files.exists(resolveMetaPath(fileId))) {
            return Optional.empty();
        }
        try {
            return Optional.of(JSON.readValue(resolveMetaPath(fileId).toFile(), StoredMetadata.class));
        } catch (IOException e) {
            throw new StorageUnavailableException("Không đọc được thuộc tính tệp", e);
        }
    }

    @Override
    public InputStream open(String fileId) {
        if (!isValidFileId(fileId)) {
            throw new StorageUnavailableException("Tệp không tồn tại", null);
        }
        try {
            return Files.newInputStream(resolveDataPath(fileId));
        } catch (NoSuchFileException e) {
            throw new StorageUnavailableException("Tệp không tồn tại", e);
        } catch (IOException e) {
            throw new StorageUnavailableException("Không đọc được tệp", e);
        }
    }

    @Override
    public void deleteFile(String fileId) {
        if (!isValidFileId(fileId)) {
            return;
        }
        try {
            Files.deleteIfExists(resolveDataPath(fileId));
            Files.deleteIfExists(resolveMetaPath(fileId));
        } catch (IOException e) {
            throw new StorageUnavailableException("Không xóa được tệp", e);
        }
    }

    private static boolean isValidFileId(String fileId) {
        return fileId != null && FILE_ID.matcher(fileId).matches();
    }

    private Path resolveDataPath(String fileId) {
        return root.resolve(fileId + ".bin");
    }

    private Path resolveMetaPath(String fileId) {
        return root.resolve(fileId + ".json");
    }
}
