package edu.aiplatform.files.service;

import edu.aiplatform.files.model.FileType;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.shared.error.ApiException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

/** F3 Tải về (P2): kiểm token đúng người, mở stream, dựng header an toàn (BR-U03-10, 21–24). */
@Service
public class DownloadService {

    static final String SVG_CSP = "default-src 'none'; style-src 'unsafe-inline'; sandbox";

    private final DownloadTokenService tokens;
    private final IStorage storage;
    private final FileAudit fileAudit;

    public DownloadService(DownloadTokenService tokens, IStorage storage, FileAudit fileAudit) {
        this.tokens = tokens;
        this.storage = storage;
        this.fileAudit = fileAudit;
    }

    public record Download(StoredMetadata metadata, InputStream content, HttpHeaders headers) {
    }

    public Download open(Actor actor, String token) {
        DownloadTokenService.Grant grant = tokens.resolve(token)
                .orElseThrow(() -> ApiException.notFound("DOWNLOAD_TOKEN_NOT_FOUND"));
        if (!grant.accountId().equals(actor.accountId())) {
            fileAudit.downloadWrongAccount(actor.accountId());
            throw ApiException.notFound("DOWNLOAD_TOKEN_NOT_FOUND");
        }
        try {
            StoredMetadata metadata = storage.metadata(grant.fileId())
                    .orElseThrow(() -> ApiException.notFound("FILE_NOT_FOUND"));
            InputStream content = storage.open(grant.fileId());
            return new Download(metadata, content, headers(metadata));
        } catch (FileAbusiveException e) {
            fileAudit.downloadAbusive(actor.accountId());
            throw ApiException.gone("FILE_UNAVAILABLE", "Tệp không khả dụng");
        } catch (StorageUnavailableException e) {
            throw ApiException.unavailable("STORAGE_UNAVAILABLE", "Lưu trữ tệp tạm thời không khả dụng");
        }
    }

    static HttpHeaders headers(StoredMetadata metadata) {
        FileType type = FileType.fromMediaType(metadata.mediaType()).orElse(null);
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.CONTENT_TYPE, type == null ? "application/octet-stream" : type.mediaType());
        headers.set("X-Content-Type-Options", "nosniff");
        headers.setCacheControl("private, no-store");
        ContentDisposition.Builder disposition = type != null && type.inline()
                ? ContentDisposition.inline() : ContentDisposition.attachment();
        headers.setContentDisposition(disposition.filename(metadata.originalFileName(), StandardCharsets.UTF_8).build());
        if (type == FileType.SVG) {
            headers.set("Content-Security-Policy", SVG_CSP);
        }
        return headers;
    }
}
