package edu.aiplatform.audit.contract.stub;

import edu.aiplatform.audit.contract.IAudit;
import edu.aiplatform.audit.contract.IAuditQuery;
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
}
