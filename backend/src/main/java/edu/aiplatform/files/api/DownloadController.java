package edu.aiplatform.files.api;

import edu.aiplatform.files.application.DownloadTokenService;
import edu.aiplatform.files.application.FileAudit;
import edu.aiplatform.files.domain.PurposePolicy;
import edu.aiplatform.files.port.FileUnavailableException;
import edu.aiplatform.files.port.StoragePort;
import edu.aiplatform.files.port.StorageUnavailableException;
import edu.aiplatform.files.port.StoredFileNotFoundException;
import edu.aiplatform.shared.contract.ActorRef;
import edu.aiplatform.shared.security.CurrentActor;
import edu.aiplatform.shared.web.ApiException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

/**
 * `GET /api/v1/files/download/{token}` (F3, P2): token phải thuộc người đang đăng nhập; stream tệp bộ đệm 64 KB với
 * header an toàn. SVG kèm CSP sandbox; Drive chặn tệp vì abuse → `410` và audit (BR-U03-10, 21…24).
 */
@RestController
public class DownloadController {

    static final int BUFFER_SIZE = 64 * 1024;
    static final String SVG_CSP = "default-src 'none'; style-src 'unsafe-inline'; sandbox";

    private final DownloadTokenService tokens;
    private final StoragePort storage;
    private final FileAudit audit;

    public DownloadController(DownloadTokenService tokens, StoragePort storage, FileAudit audit) {
        this.tokens = tokens;
        this.storage = storage;
        this.audit = audit;
    }

    @GetMapping("/api/v1/files/download/{token}")
    public ResponseEntity<StreamingResponseBody> download(@PathVariable String token) {
        ActorRef actor = CurrentActor.require();
        String fileId = tokens.resolve(token, actor.accountId());
        Map<String, String> meta;
        InputStream content;
        try {
            meta = storage.readMetadata(fileId);
            content = storage.get(fileId);
        } catch (FileUnavailableException e) {
            audit.downloadAbusive(actor.accountId());
            throw new ApiException(HttpStatus.GONE, "FILE_UNAVAILABLE", "Tệp không khả dụng.");
        } catch (StoredFileNotFoundException e) {
            throw new ApiException(HttpStatus.NOT_FOUND, "FILE_NOT_FOUND", "Không tìm thấy tệp.");
        } catch (StorageUnavailableException e) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "STORAGE_UNAVAILABLE",
                    "Lưu trữ tệp tạm thời không khả dụng, vui lòng thử lại sau.");
        }
        String mediaType = meta.getOrDefault(StoragePort.MEDIA_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE);
        String fileName = meta.getOrDefault(StoragePort.ORIGINAL_FILE_NAME, "file");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(mediaType));
        headers.setContentDisposition((PurposePolicy.isInline(mediaType)
                ? ContentDisposition.inline() : ContentDisposition.attachment())
                .filename(fileName, StandardCharsets.UTF_8).build());
        headers.set("X-Content-Type-Options", "nosniff");
        headers.setCacheControl(CacheControl.noStore().cachePrivate());
        if (PurposePolicy.SVG.equals(mediaType)) {
            headers.set("Content-Security-Policy", SVG_CSP);
        }
        String size = meta.get(StoragePort.BYTE_SIZE);
        if (size != null && !"0".equals(size)) {
            headers.setContentLength(Long.parseLong(size));
        }

        StreamingResponseBody body = out -> {
            try (InputStream in = content) {
                byte[] buffer = new byte[BUFFER_SIZE];
                int n;
                while ((n = in.read(buffer)) != -1) {
                    out.write(buffer, 0, n);
                }
            }
        };
        return ResponseEntity.ok().headers(headers).body(body);
    }
}
