package edu.aiplatform.files.application;

import edu.aiplatform.audit.port.AuditEvent;
import edu.aiplatform.audit.port.AuditPort;
import edu.aiplatform.audit.port.AuditResult;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Audit của U03 (BR-U03-40, Bước 10): upload bị từ chối, token dùng sai người, Drive chặn tệp vì abuse.
 * Không audit lần tải thành công; không ghi `fileId`, khóa hay token.
 */
@Component
public class FileAudit {

    static final String UPLOAD = "FILE_UPLOAD";
    static final String DOWNLOAD = "FILE_DOWNLOAD";

    private final AuditPort audit;

    public FileAudit(AuditPort audit) {
        this.audit = audit;
    }

    public void uploadDenied(UUID actorId, String reason, Map<String, Object> details) {
        audit.recordDenied(event(actorId, UPLOAD, AuditResult.DENIED, reason, details));
    }

    public void downloadWrongAccount(UUID actorId) {
        audit.recordDenied(event(actorId, DOWNLOAD, AuditResult.DENIED, "TOKEN_ACCOUNT_MISMATCH", Map.of()));
    }

    public void downloadAbusive(UUID actorId) {
        audit.recordFailure(event(actorId, DOWNLOAD, AuditResult.FAILURE, "DRIVE_ABUSE_BLOCKED", Map.of()));
    }

    private static AuditEvent event(UUID actorId, String action, AuditResult result, String reason,
                                    Map<String, Object> details) {
        return new AuditEvent(UUID.randomUUID(), actorId, action, "FILE", null, result, reason, details, Instant.now());
    }
}
