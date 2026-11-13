package edu.aiplatform.audit.contract.stub;

import edu.aiplatform.audit.contract.IAudit;
import edu.aiplatform.audit.contract.IAuditQuery;
import edu.aiplatform.audit.contract.IAuditStats;
import java.time.Instant;
import java.util.List;
import edu.aiplatform.shared.stub.Stubs;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.Page;

/** Bản tạm của interface U02: audit chỉ ghi ra log (không ghi {@code details}), tra cứu trả trang rỗng. */
@AutoConfiguration
@ConditionalOnProperty(name = "platform.contract-stubs.enabled", havingValue = "true", matchIfMissing = true)
public class AuditContractStubs {

    @Bean
    @ConditionalOnMissingBean
    IAudit auditStub() {
        return Stubs.announce(IAudit.class, event -> Stubs.called(IAudit.class,
                event.action() + " " + event.result() + " actor=" + event.actorId()
                        + " object=" + event.objectType() + ":" + event.objectId()));
    }

    @Bean
    @ConditionalOnMissingBean
    IAuditQuery auditQueryStub() {
        return Stubs.announce(IAuditQuery.class, (actor, filter, page, recordView) -> Page.empty(page));
    }

    @Bean
    @ConditionalOnMissingBean
    IAuditStats auditStatsStub() {
        return Stubs.announce(IAuditStats.class, new IAuditStats() {
            @Override
            public long countSucceeded(String action, Instant from, Instant to, String detailKey, String detailValue) {
                return 0;
            }

            @Override
            public List<edu.aiplatform.audit.contract.AuditLogView> recentSecurityEvents(Instant from, int limit) {
                return List.of();
            }
        });
    }
}
