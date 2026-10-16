package edu.aiplatform.identity.contract;

import java.util.UUID;

/** Người thực hiện thao tác, dựng phía server từ phiên đăng nhập hoặc credential MCP; không lấy từ request. */
public record Actor(UUID accountId, Role role) {
}
