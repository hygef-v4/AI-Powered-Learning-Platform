package edu.aiplatform.audit.application;

import edu.aiplatform.audit.port.*;
import org.springframework.stereotype.Service;

/**
 * Khung cài đặt của U02: thân hàm tạm trả giá trị rỗng/an toàn hoặc báo chưa cài.
 */
@Service
public class AuditStore implements AuditPort {

    @Override
    public void record(AuditEvent event) {
        // Chưa cài: U02.
    }

    @Override
    public void recordDenied(AuditEvent event) {
        // Chưa cài: U02.
    }

    @Override
    public void recordFailure(AuditEvent event) {
        // Chưa cài: U02.
    }
}
