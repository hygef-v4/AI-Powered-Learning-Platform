package edu.aiplatform.files.infrastructure;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import edu.aiplatform.files.application.FilesProperties;
import edu.aiplatform.files.port.ArtifactPurpose;
import edu.aiplatform.files.port.FileUnavailableException;
import edu.aiplatform.files.port.StoragePort;
import edu.aiplatform.files.port.StorageUnavailableException;
import edu.aiplatform.files.port.StorageWriteException;
import edu.aiplatform.files.port.StoredFileNotFoundException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Lưu tệp trên Google Shared Drive bằng JSON key của service account (Bước 15, P5, P6).
 * Mỗi `purpose` một thư mục (tạo khi khởi động, hoặc khi upload nếu lúc khởi động Drive lỗi). Tên tệp gốc nằm ở `name` của tệp Drive; các metadata còn lại ở
 * `appProperties` (giới hạn 124 byte mỗi cặp nên không chứa được tên dài). Tệp và metadata tạo trong một request.
 * Timeout: kết nối 5 s, đọc 60 s, upload 120 s; 429/5xx retry 3 lần (1, 2, 4 s, có jitter); 401/403/404/abuse
 * không retry. Key sai hoặc Drive lỗi khi khởi động: backend vẫn chạy, thao tác tệp báo tạm thời không khả dụng.
 */
@Component
@ConditionalOnExpression("!'${platform.files.drive.service-account-key:}'.isBlank()")
public class GoogleDriveStorageAdapter implements StoragePort {

    private static final Logger log = LoggerFactory.getLogger(GoogleDriveStorageAdapter.class);
    private static final String FOLDER_MIME = "application/vnd.google-apps.folder";
    private static final String ABUSE_REASON = "cannotDownloadAbusiveFile";
    private static final int MAX_RETRIES = 3;
    static final int CONNECT_TIMEOUT_MS = 5_000;
    static final int READ_TIMEOUT_MS = 60_000;
    static final int UPLOAD_TIMEOUT_MS = 120_000;

    private final String sharedDriveId;
    private final Drive drive;
    private final Drive uploadDrive;
    private final Map<ArtifactPurpose, String> folders = new EnumMap<>(ArtifactPurpose.class);

    public GoogleDriveStorageAdapter(FilesProperties properties) {
        this.sharedDriveId = properties.drive().sharedDriveId();
        Drive read = null;
        Drive upload = null;
        try {
            byte[] json = Base64.getDecoder().decode(properties.drive().serviceAccountKey().strip());
            GoogleCredentials credentials = GoogleCredentials.fromStream(new ByteArrayInputStream(json))
                    .createScoped(List.of(DriveScopes.DRIVE));
            HttpTransport transport = GoogleNetHttpTransport.newTrustedTransport();
            read = client(transport, credentials, READ_TIMEOUT_MS);
            upload = client(transport, credentials, UPLOAD_TIMEOUT_MS);
        } catch (Exception e) {
            log.error("Google Drive client could not be created; file storage is unavailable: {}", e.getClass().getName());
        }
        this.drive = read;
        this.uploadDrive = upload;
    }

    /** Tạo sẵn thư mục của từng `purpose` khi khởi động (NFR-U03-11); lỗi chỉ log, lần upload sau sẽ thử lại. */
    @EventListener(ApplicationReadyEvent.class)
    public void createFoldersOnStartup() {
        if (drive == null) {
            return;
        }
        Thread warmUp = new Thread(() -> {
            for (ArtifactPurpose purpose : ArtifactPurpose.values()) {
                try {
                    folderFor(purpose);
                } catch (RuntimeException e) {
                    log.warn("Could not prepare Drive folder {}: {}", purpose, e.getClass().getSimpleName());
                }
            }
        }, "drive-folders");
        warmUp.setDaemon(true);
        warmUp.start();
    }

    private static Drive client(HttpTransport transport, GoogleCredentials credentials, int readTimeoutMs) {
        HttpCredentialsAdapter auth = new HttpCredentialsAdapter(credentials);
        HttpRequestInitializer init = request -> {
            auth.initialize(request);
            request.setConnectTimeout(CONNECT_TIMEOUT_MS);
            request.setReadTimeout(readTimeoutMs);
        };
        return new Drive.Builder(transport, GsonFactory.getDefaultInstance(), init)
                .setApplicationName("ai-learning-platform")
                .build();
    }

    @Override
    public String put(Path content, Map<String, String> metadata) {
        Drive client = require(uploadDrive);
        ArtifactPurpose purpose = ArtifactPurpose.valueOf(metadata.get(PURPOSE));
        String mediaType = metadata.get(MEDIA_TYPE);
        Map<String, String> appProperties = new LinkedHashMap<>(metadata);
        appProperties.remove(ORIGINAL_FILE_NAME);
        File file = new File()
                .setName(metadata.get(ORIGINAL_FILE_NAME))
                .setMimeType(mediaType)
                .setParents(List.of(folderFor(purpose)))
                .setAppProperties(appProperties);
        File created = withRetry(() -> client.files()
                .create(file, new FileContent(mediaType, content.toFile()))
                .setSupportsAllDrives(true)
                .setFields("id,size")
                .execute());
        long expected = size(content);
        if (created.getSize() == null || created.getSize() != expected) {
            throw new StorageWriteException("Uploaded size mismatch", created.getId(), null);
        }
        return created.getId();
    }

    @Override
    public InputStream get(String fileId) {
        Drive client = require(drive);
        return withRetry(() -> client.files().get(fileId).setSupportsAllDrives(true).executeMediaAsInputStream());
    }

    @Override
    public Map<String, String> readMetadata(String fileId) {
        Drive client = require(drive);
        File file = withRetry(() -> client.files().get(fileId)
                .setSupportsAllDrives(true)
                .setFields("name,size,appProperties")
                .execute());
        Map<String, String> result = new LinkedHashMap<>();
        if (file.getAppProperties() != null) {
            result.putAll(file.getAppProperties());
        }
        result.put(ORIGINAL_FILE_NAME, file.getName());
        result.put(BYTE_SIZE, file.getSize() == null ? "0" : file.getSize().toString());
        return result;
    }

    @Override
    public void delete(String fileId) {
        Drive client = require(drive);
        try {
            withRetry(() -> {
                client.files().delete(fileId).setSupportsAllDrives(true).execute();
                return null;
            });
        } catch (StoredFileNotFoundException e) {
            // Đã không còn: coi như xong (việc dọn idempotent).
        }
    }

    /** P7: kiểm Shared Drive còn truy cập được. */
    public void checkSharedDrive() {
        Drive client = require(drive);
        withRetry(() -> client.drives().get(sharedDriveId).setFields("id").execute());
    }

    private synchronized String folderFor(ArtifactPurpose purpose) {
        String id = folders.get(purpose);
        if (id == null) {
            id = withRetry(() -> findOrCreateFolder(purpose.name()));
            folders.put(purpose, id);
        }
        return id;
    }

    private String findOrCreateFolder(String name) throws IOException {
        FileList found = drive.files().list()
                .setCorpora("drive")
                .setDriveId(sharedDriveId)
                .setIncludeItemsFromAllDrives(true)
                .setSupportsAllDrives(true)
                .setQ("mimeType='" + FOLDER_MIME + "' and name='" + name + "' and '" + sharedDriveId
                        + "' in parents and trashed=false")
                .setFields("files(id)")
                .execute();
        if (found.getFiles() != null && !found.getFiles().isEmpty()) {
            return found.getFiles().get(0).getId();
        }
        File folder = new File().setName(name).setMimeType(FOLDER_MIME).setParents(List.of(sharedDriveId));
        return drive.files().create(folder).setSupportsAllDrives(true).setFields("id").execute().getId();
    }

    @FunctionalInterface
    private interface DriveCall<T> {
        T call() throws IOException;
    }

    private static <T> T withRetry(DriveCall<T> call) {
        IOException last = null;
        for (int attempt = 0; attempt <= MAX_RETRIES; attempt++) {
            try {
                return call.call();
            } catch (GoogleJsonResponseException e) {
                int status = e.getStatusCode();
                if (status == 404) {
                    throw new StoredFileNotFoundException("No such file");
                }
                if (isAbuse(e)) {
                    throw new FileUnavailableException("File blocked by Google Drive");
                }
                if (status != 429 && status < 500) {
                    throw new StorageUnavailableException("Google Drive rejected the request: " + status, null);
                }
                last = e;
            } catch (IOException e) {
                last = e;
            }
            if (attempt < MAX_RETRIES) {
                sleep(attempt);
            }
        }
        throw new StorageUnavailableException("Google Drive unavailable", last);
    }

    private static boolean isAbuse(GoogleJsonResponseException e) {
        GoogleJsonError details = e.getDetails();
        return details != null && details.getErrors() != null
                && details.getErrors().stream().anyMatch(err -> ABUSE_REASON.equals(err.getReason()));
    }

    /** Backoff 1, 2, 4 s có jitter ±20%. */
    private static void sleep(int attempt) {
        long base = 1000L << attempt;
        long jitter = (long) (base * 0.2 * (ThreadLocalRandom.current().nextDouble() * 2 - 1));
        try {
            Thread.sleep(base + jitter);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new StorageUnavailableException("Interrupted", e);
        }
    }

    private static Drive require(Drive client) {
        if (client == null) {
            throw new StorageUnavailableException("Google Drive is not configured correctly", null);
        }
        return client;
    }

    private static long size(Path path) {
        try {
            return Files.size(path);
        } catch (IOException e) {
            throw new StorageUnavailableException("Cannot read temp file", e);
        }
    }
}
