package edu.aiplatform.aiexecution.application;

import edu.aiplatform.billing.port.*;
import edu.aiplatform.content.port.*;
import edu.aiplatform.shared.contract.Page;
import edu.aiplatform.shared.contract.PageRequest;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Khung cài đặt của U13: thân hàm tạm trả giá trị rỗng/an toàn hoặc báo chưa cài.
 * Plan U13: Bước 9.
 */
@Service
public class AiUsageService implements AiUsagePort, CreditUsagePort {

    @Override
    public AiUsageTicket begin(String taskType, UUID requestedBy, String targetType, UUID targetId, String requestRef) {
        throw new UnsupportedOperationException("Chưa cài: U13");
    }

    @Override
    public void complete(UUID suggestionId, long inputTokens, long outputTokens, BigDecimal costUsd) {
        // Chưa cài: U13.
    }

    @Override
    public void fail(UUID suggestionId) {
        // Chưa cài: U13.
    }

    @Override
    public Page<CreditUsage> listUsage(UUID accountId, PageRequest page) {
        return new Page<>(List.of(), 0, page.page(), page.size());
    }
}
