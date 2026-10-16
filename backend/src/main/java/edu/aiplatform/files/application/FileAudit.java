package edu.aiplatform.files.application;

import edu.aiplatform.audit.contract.AuditEvent;
import edu.aiplatform.audit.contract.AuditResult;
import edu.aiplatform.audit.contract.IAudit;
import java.time.Clock;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Audit của phần tệp (BR-U03-40): upload bị từ chối, abuse, token sai người. Không audit lần tải thành công. */
@Component
public class FileAudit {

    static final String UPLOAD = "FILE_UPLOAD";
    static final String DOWNLOAD = "FILE_DOWNLOAD";

    private final IAudit audit;
    private final Clock clock;

    public FileAudit(IAudit audit, Clock clock) {
        this.audit = audit;
        this.clock = clock;
    }

    public void uploadDenied(UUID actorId, String reason, Map<String, Object> details) {
        record(actorId, UPLOAD, AuditResult.DENIED, reason, details);
    }

    public void downloadWrongAccount(UUID actorId) {
        record(actorId, DOWNLOAD, AuditResult.DENIED, "TOKEN_ACCOUNT_MISMATCH", Map.of());
    }

    public void downloadAbusive(UUID actorId) {
        record(actorId, DOWNLOAD, AuditResult.FAILURE, "DRIVE_ABUSE_BLOCKED", Map.of());
    }

    private void record(UUID actorId, String action, AuditResult result, String reason, Map<String, Object> details) {
        audit.record(new AuditEvent(UUID.randomUUID(), actorId, action, "FILE", null, result, reason, details,
                clock.instant()));
    }
}
