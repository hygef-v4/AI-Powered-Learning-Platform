package edu.aiplatform.audit.application;

import edu.aiplatform.audit.port.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Khung cài đặt của U02: tới khi U02 ghi bảng `audit_logs`, sự kiện audit chỉ được ghi ra log
 * (adapter giả theo plan U03 Bước J3). Không ghi `details` vì có thể chứa dữ liệu chưa được che.
 */
@Service
public class AuditStore implements AuditPort {

    private static final Logger log = LoggerFactory.getLogger("audit");

    @Override
    public void record(AuditEvent event) {
        write(event);
    }

    @Override
    public void recordDenied(AuditEvent event) {
        write(event);
    }

    @Override
    public void recordFailure(AuditEvent event) {
        write(event);
    }

    private static void write(AuditEvent event) {
        log.info("audit action={} result={} actor={} object={}:{} reason={}", event.action(), event.result(),
                event.actorId(), event.objectType(), event.objectId(), event.reason());
    }
}
