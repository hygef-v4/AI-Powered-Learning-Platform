package edu.aiplatform.files.infrastructure;

import edu.aiplatform.files.application.FilesProperties;
import edu.aiplatform.files.port.StoragePort;
import edu.aiplatform.files.port.StorageUnavailableException;
import edu.aiplatform.files.port.StorageWriteException;
import edu.aiplatform.files.port.StoredFileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

/**
 * Lưu tệp vào thư mục local khi không có key Google Drive và không bắt buộc Drive (`U03_REQUIRE_DRIVE=false`)
 * (P5, NFR-U03-31): chỉ dùng khi chạy local và trong test.
 * Mỗi tệp gồm `{id}.bin` và `{id}.properties` (metadata).
 */
@Component
@ConditionalOnExpression("'${platform.files.drive.service-account-key:}'.isBlank() and !${platform.files.require-drive:false}")
public class LocalFolderStorageAdapter implements StoragePort {

    private static final Logger log = LoggerFactory.getLogger(LocalFolderStorageAdapter.class);
    private static final Pattern ID = Pattern.compile("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");

    private final Path root;

    @Autowired
    public LocalFolderStorageAdapter(FilesProperties properties) {
        this(properties.localStorageDir());
        log.warn("Google Drive is not configured; files are stored locally in {} (local/test only)", root);
    }

    public LocalFolderStorageAdapter(Path root) {
        this.root = root.toAbsolutePath().normalize();
    }

    @Override
    public String put(Path content, Map<String, String> metadata) {
        String id = UUID.randomUUID().toString();
        try {
            Files.createDirectories(root);
            Files.copy(content, data(id), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            deleteQuietly(id);
            throw new StorageUnavailableException("Local storage write failed", e);
        }
        try {
            Properties props = new Properties();
            props.putAll(metadata);
            props.setProperty(BYTE_SIZE, Long.toString(Files.size(data(id))));
            try (Writer out = Files.newBufferedWriter(meta(id), StandardCharsets.UTF_8)) {
                props.store(out, null);
            }
        } catch (IOException e) {
            throw new StorageWriteException("Local metadata write failed", id, e);
        }
        return id;
    }

    @Override
    public InputStream get(String fileId) {
        try {
            return Files.newInputStream(data(fileId));
        } catch (NoSuchFileException e) {
            throw new StoredFileNotFoundException("No such file");
        } catch (IOException e) {
            throw new StorageUnavailableException("Local storage read failed", e);
        }
    }

    @Override
    public Map<String, String> readMetadata(String fileId) {
        Properties props = new Properties();
        try (Reader in = Files.newBufferedReader(meta(fileId), StandardCharsets.UTF_8)) {
            props.load(in);
        } catch (NoSuchFileException e) {
            throw new StoredFileNotFoundException("No such file");
        } catch (IOException e) {
            throw new StorageUnavailableException("Local storage read failed", e);
        }
        Map<String, String> result = new LinkedHashMap<>();
        props.stringPropertyNames().forEach(k -> result.put(k, props.getProperty(k)));
        return result;
    }

    @Override
    public void delete(String fileId) {
        try {
            Files.deleteIfExists(data(fileId));
            Files.deleteIfExists(meta(fileId));
        } catch (IOException e) {
            throw new StorageUnavailableException("Local storage delete failed", e);
        }
    }

    /** `fileId` phải là UUID do adapter sinh, chặn path traversal. */
    private Path data(String fileId) {
        return root.resolve(checked(fileId) + ".bin");
    }

    private Path meta(String fileId) {
        return root.resolve(checked(fileId) + ".properties");
    }

    private static String checked(String fileId) {
        if (fileId == null || !ID.matcher(fileId).matches()) {
            throw new StoredFileNotFoundException("No such file");
        }
        return fileId;
    }

    private void deleteQuietly(String id) {
        try {
            Files.deleteIfExists(data(id));
        } catch (IOException ignored) {
            // Không còn gì để làm; tệp local mồ côi không ảnh hưởng người dùng.
        }
    }
}
