package edu.aiplatform.identity.service;

import edu.aiplatform.identity.contract.Role;
import java.util.UUID;

/** Kết quả đăng nhập hoặc kích hoạt trả cho frontend (domain-entities U01 mục 14). */
public record SessionInfo(UUID accountId, String displayName, Role role, String homePath) {
}
