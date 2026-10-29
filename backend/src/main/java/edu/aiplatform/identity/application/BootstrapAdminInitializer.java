package edu.aiplatform.identity.application;

import edu.aiplatform.audit.contract.AuditEvent;
import edu.aiplatform.audit.contract.AuditResult;
import edu.aiplatform.audit.contract.IAudit;
import edu.aiplatform.identity.contract.Role;
import edu.aiplatform.identity.domain.Account;
import edu.aiplatform.identity.domain.AccountRepository;
import edu.aiplatform.identity.domain.EmailPolicy;
import edu.aiplatform.identity.domain.ProfilePolicy;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tạo Admin đầu tiên khi chưa có Admin nào (B14): email từ {@code U01_BOOTSTRAP_ADMIN_EMAIL}, trạng thái
 * {@code PENDING}, không mật khẩu mặc định (SECURITY-09). Admin tự kích hoạt bằng UC 01. Chạy lại không tạo trùng;
 * worker không chạy.
 */
@Component
@Profile("!worker")
public class BootstrapAdminInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BootstrapAdminInitializer.class);

    private final AccountRepository accounts;
    private final IAudit audit;
    private final IdentityProperties properties;
    private final Clock clock;

    public BootstrapAdminInitializer(AccountRepository accounts, IAudit audit, IdentityProperties properties,
                                     Clock clock) {
        this.accounts = accounts;
        this.audit = audit;
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        String email = EmailPolicy.normalize(properties.bootstrapAdminEmail());
        if (email.isEmpty() || accounts.anyAdminExists()) {
            return;
        }
        if (new EmailPolicy(properties.allowedEmailDomains()).check(email) != EmailPolicy.EmailCheck.OK) {
            log.error("U01_BOOTSTRAP_ADMIN_EMAIL không thuộc U01_ALLOWED_EMAIL_DOMAINS; không tạo Admin đầu tiên");
            return;
        }
        String name = ProfilePolicy.normalizeDisplayName(properties.bootstrapAdminName());
        if (ProfilePolicy.checkDisplayName(name).isPresent()) {
            name = "Quản trị viên";
        }
        Instant now = clock.instant();
        Account admin = Account.newPending(UUID.randomUUID(), email, Role.ADMIN, name, null, now);
        accounts.insert(admin);
        audit.record(new AuditEvent(UUID.randomUUID(), null, "ACCOUNT_CREATED", "ACCOUNT", admin.id().toString(),
                AuditResult.SUCCESS, "BOOTSTRAP_ADMIN", Map.of("role", Role.ADMIN.name()), now));
        log.info("Đã tạo Admin đầu tiên ở trạng thái chờ kích hoạt");
    }
}
