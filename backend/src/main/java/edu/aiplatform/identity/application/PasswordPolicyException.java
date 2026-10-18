package edu.aiplatform.identity.application;

import edu.aiplatform.identity.domain.PasswordPolicy;
import java.util.Set;

/** Mật khẩu mới không đạt chính sách (BR-U01-30, 31); trả 400 {@code PASSWORD_POLICY} kèm danh sách vi phạm. */
public class PasswordPolicyException extends RuntimeException {

    private final Set<PasswordPolicy.Violation> violations;

    public PasswordPolicyException(Set<PasswordPolicy.Violation> violations) {
        super("Mật khẩu chưa đạt yêu cầu");
        this.violations = Set.copyOf(violations);
    }

    public Set<PasswordPolicy.Violation> violations() {
        return violations;
    }
}
