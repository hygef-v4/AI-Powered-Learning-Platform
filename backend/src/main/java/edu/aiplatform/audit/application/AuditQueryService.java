package edu.aiplatform.audit.application;

import edu.aiplatform.audit.port.*;
import edu.aiplatform.shared.contract.ActorRef;
import edu.aiplatform.shared.contract.Page;
import edu.aiplatform.shared.contract.PageRequest;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Khung cài đặt của U02: thân hàm tạm trả giá trị rỗng/an toàn hoặc báo chưa cài.
 */
@Service
public class AuditQueryService implements AuditQueryPort {

    @Override
    public Page<AuditEvent> query(ActorRef actor, AuditFilter filter, PageRequest page) {
        return new Page<>(List.of(), 0, page.page(), page.size());
    }
}
