package edu.aiplatform.identity.domain;

import static org.assertj.core.api.Assertions.assertThat;

import edu.aiplatform.identity.domain.EmailPolicy.EmailCheck;
import java.util.List;
import org.junit.jupiter.api.Test;

class EmailPolicyTest {

    private final EmailPolicy policy = new EmailPolicy(List.of(" FPT.edu.vn ", "@fe.edu.vn"));

    @Test
    void normalizesBeforeComparing() {
        assertThat(EmailPolicy.normalize("  TaiVM@FPT.edu.VN \t")).isEqualTo("taivm@fpt.edu.vn");
        assertThat(EmailPolicy.normalize(null)).isEmpty();
    }

    @Test
    void acceptsOnlyConfiguredDomains() {
        assertThat(policy.check("taivm@fpt.edu.vn")).isEqualTo(EmailCheck.OK);
        assertThat(policy.check("gv@fe.edu.vn")).isEqualTo(EmailCheck.OK);
        assertThat(policy.check("taivm@gmail.com")).isEqualTo(EmailCheck.EMAIL_DOMAIN_NOT_ALLOWED);
        assertThat(policy.check("taivm@sub.fpt.edu.vn")).isEqualTo(EmailCheck.EMAIL_DOMAIN_NOT_ALLOWED);
    }

    @Test
    void rejectsMalformedEmail() {
        assertThat(policy.check("")).isEqualTo(EmailCheck.INVALID_EMAIL);
        assertThat(policy.check("taivm")).isEqualTo(EmailCheck.INVALID_EMAIL);
        assertThat(policy.check("tai vm@fpt.edu.vn")).isEqualTo(EmailCheck.INVALID_EMAIL);
        assertThat(policy.check("a".repeat(250) + "@fpt.edu.vn")).isEqualTo(EmailCheck.INVALID_EMAIL);
    }

    @Test
    void emptyConfigurationAllowsNoDomain() {
        assertThat(new EmailPolicy(List.of()).check("taivm@fpt.edu.vn"))
                .isEqualTo(EmailCheck.EMAIL_DOMAIN_NOT_ALLOWED);
    }
}
