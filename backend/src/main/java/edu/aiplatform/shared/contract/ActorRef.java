package edu.aiplatform.shared.contract;

import java.util.UUID;

/** Người thực hiện thao tác, dựng từ access token (U01). */
public record ActorRef(UUID accountId, Role role) {}
