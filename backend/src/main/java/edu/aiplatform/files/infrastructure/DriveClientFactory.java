package edu.aiplatform.files.infrastructure;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import edu.aiplatform.files.application.FilesProperties;
import edu.aiplatform.files.application.StorageUnavailableException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Component;

/**
 * Tạo client Drive từ JSON key service account (base64) và gọi Drive có thử lại (P6): 429, 5xx và lỗi mạng
 * thử lại 3 lần, backoff 1, 2, 4 giây có jitter; lỗi quyền hoặc abuse không thử lại.
 */
@Component
public class DriveClientFactory {

    static final int CONNECT_TIMEOUT_MS = 5_000;
    static final int READ_TIMEOUT_MS = 120_000;

    private final FilesProperties properties;
    private volatile Drive drive;

    public DriveClientFactory(FilesProperties properties) {
        this.properties = properties;
    }

    public Drive drive() {
        Drive current = drive;
        if (current == null) {
            synchronized (this) {
                if (drive == null) {
                    drive = create(properties.driveServiceAccountKey());
                }
                current = drive;
            }
        }
        return current;
    }

    static Drive create(String base64Key) {
        try {
            GoogleCredentials credentials = GoogleCredentials
                    .fromStream(new ByteArrayInputStream(Base64.getDecoder().decode(base64Key.strip())))
                    .createScoped(List.of(DriveScopes.DRIVE));
            HttpCredentialsAdapter adapter = new HttpCredentialsAdapter(credentials);
            HttpRequestInitializer initializer = request -> {
                adapter.initialize(request);
                request.setConnectTimeout(CONNECT_TIMEOUT_MS);
                request.setReadTimeout(READ_TIMEOUT_MS);
            };
            return new Drive.Builder(GoogleNetHttpTransport.newTrustedTransport(), GsonFactory.getDefaultInstance(),
                    initializer).setApplicationName("alap-platform").build();
        } catch (IOException | GeneralSecurityException | IllegalArgumentException e) {
            throw new StorageUnavailableException("Cấu hình Google Drive không hợp lệ", e);
        }
    }

    /** Gọi Drive có thử lại; hết lượt thì ném {@link StorageUnavailableException}. */
    public static <T> T withRetry(Callable<T> call) {
        long[] backoffMs = {1_000, 2_000, 4_000};
        for (int attempt = 0; ; attempt++) {
            try {
                return call.call();
            } catch (GoogleJsonResponseException e) {
                if (!retryable(e.getStatusCode()) || attempt >= backoffMs.length) {
                    throw e.getStatusCode() == 404 ? new DriveNotFoundException(e)
                            : new StorageUnavailableException("Drive trả lỗi " + e.getStatusCode(), e);
                }
                sleep(backoffMs[attempt]);
            } catch (IOException e) {
                if (attempt >= backoffMs.length) {
                    throw new StorageUnavailableException("Không kết nối được Drive", e);
                }
                sleep(backoffMs[attempt]);
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new StorageUnavailableException("Lỗi gọi Drive", e);
            }
        }
    }

    static boolean retryable(int status) {
        return status == 429 || status >= 500;
    }

    private static void sleep(long baseMs) {
        try {
            Thread.sleep(baseMs + ThreadLocalRandom.current().nextLong(250));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new StorageUnavailableException("Bị ngắt khi chờ thử lại", e);
        }
    }

    /** Drive báo không tìm thấy tệp hoặc thư mục. */
    public static class DriveNotFoundException extends StorageUnavailableException {

        DriveNotFoundException(Throwable cause) {
            super("Drive không tìm thấy", cause);
        }
    }
}
