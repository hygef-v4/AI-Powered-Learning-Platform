package edu.aiplatform.audit.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.aiplatform.audit.contract.AuditEvent;
import edu.aiplatform.audit.contract.AuditResult;
import edu.aiplatform.audit.contract.IAudit;
import edu.aiplatform.audit.domain.AuditLogRepository;
import edu.aiplatform.audit.domain.AuditRow;
import edu.aiplatform.shared.security.ForbiddenKeyGuard;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Cài {@link IAudit} (A1). {@code SUCCESS} ghi ngay trong transaction của unit gọi: nghiệp vụ rollback thì audit
 * mất theo. {@code DENIED}, {@code FAILURE} ghi bằng transaction riêng để còn lại khi nghiệp vụ rollback (BR-U02-02).
 * Lỗi ghi không bị nuốt nên thao tác cùng transaction rollback (BR-U02-04).
 */
@Service
public class AuditStore implements IAudit {

    private final AuditLogRepository repository;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate separateTransaction;

    public AuditStore(AuditLogRepository repository, ObjectMapper objectMapper,
                      PlatformTransactionManager transactionManager) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.separateTransaction = new TransactionTemplate(transactionManager);
        this.separateTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Override
    public void record(AuditEvent event) {
        AuditRow row = toRow(event);
        if (event.result() == AuditResult.SUCCESS) {
            repository.insertIfAbsent(row);
        } else {
            separateTransaction.executeWithoutResult(status -> repository.insertIfAbsent(row));
        }
    }

    /** Kiểm sự kiện trước khi ghi; sai là lỗi lập trình của unit gọi nên ném {@link IllegalArgumentException}. */
    private AuditRow toRow(AuditEvent event) {
        if (event == null || event.id() == null || event.result() == null || event.occurredAt() == null
                || event.action() == null || event.action().isBlank()) {
            throw new IllegalArgumentException("Sự kiện audit thiếu id, action, result hoặc occurredAt");
        }
        Map<String, Object> details = event.details() == null ? Map.of() : event.details();
        ForbiddenKeyGuard.check(details);
        return new AuditRow(event.id(), event.actorId(), event.action(), event.objectType(), event.objectId(),
                event.result().name(), event.reason(), toJson(details), event.occurredAt());
    }

    private String toJson(Map<String, Object> details) {
        try {
            return objectMapper.writeValueAsString(details);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("details của audit không chuyển được sang JSON", e);
        }
    }
}
