package edu.aiplatform.identity.port;

import edu.aiplatform.shared.contract.Role;
import java.util.UUID;

/** Không chứa mật khẩu hay số điện thoại. */
public record AccountContact(UUID accountId, String schoolEmail, String displayName, Role role, AccountStatus status) {}
