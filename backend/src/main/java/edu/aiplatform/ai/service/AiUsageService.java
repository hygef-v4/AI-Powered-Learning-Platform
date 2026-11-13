package edu.aiplatform.ai.service;

import edu.aiplatform.ai.service.IProviderGateway.Usage;
import edu.aiplatform.ai.contract.AiProvider;
import edu.aiplatform.ai.contract.UsageMeasurement;
import edu.aiplatform.ai.model.AiUsageRecord;
import edu.aiplatform.ai.model.AiUsageRecord.Source;
import edu.aiplatform.ai.repository.AiUsageRepository;
import edu.aiplatform.identity.contract.Actions;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.shared.error.ApiException;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ghi và đọc usage (F7). Không biết số token thì để rỗng với {@code UNAVAILABLE}, không ghi 0 (BR-U06-35); không có
 * prompt, key hay nội dung (BR-U06-40).
 */
@Service
public class AiUsageService {

    public static final int MAX_PAGE = 100;

    private final AiUsageRepository repository;
    private final AiAccess access;
    private final Clock clock;

    public AiUsageService(AiUsageRepository repository, AiAccess access, Clock clock) {
        this.repository = repository;
        this.access = access;
        this.clock = clock;
    }

    /** Usage của một lời gọi provider: có số thì {@code PROVIDER_MEASURED}, không thì theo nhãn truyền vào. */
    public void recordProvider(UUID accountId, Source source, UUID taskId, AiProvider provider, String model,
                               String taskType, String outcome, Usage usage, Long estimated) {
        boolean measured = usage != null && (usage.inputTokens() != null || usage.outputTokens() != null);
        UsageMeasurement provenance = measured ? UsageMeasurement.PROVIDER_MEASURED
                : estimated != null ? UsageMeasurement.ESTIMATED : UsageMeasurement.UNAVAILABLE;
        repository.insert(new AiUsageRecord(UUID.randomUUID(), accountId, source, taskId, null, provider, model, taskType,
                null, outcome, measured ? usage.inputTokens() : null,
                measured ? usage.outputTokens() : estimated, measured ? usage.cachedTokens() : null,
                measured ? usage.reasoningTokens() : null, provenance, clock.instant()));
    }

    /** Usage một lời gọi tool MCP; token chỉ có khi agent tự báo (BR-U06-35). */
    public void recordMcp(UUID accountId, UUID credentialId, String toolName, String outcome, Long inputTokens,
                          Long outputTokens) {
        boolean reported = inputTokens != null || outputTokens != null;
        repository.insert(new AiUsageRecord(UUID.randomUUID(), accountId, Source.MCP, null, credentialId, null, null, null,
                toolName, outcome, inputTokens, outputTokens, null, null,
                reported ? UsageMeasurement.EXTERNALLY_REPORTED : UsageMeasurement.UNAVAILABLE, clock.instant()));
    }

    /** Lịch sử của chính người gọi (UC 33), mới nhất trước, tối đa 100 dòng mỗi trang. */
    @Transactional(readOnly = true)
    public UsagePage listOwn(Actor actor, Source source, Instant from, Instant to, int page, int size) {
        access.require(actor, Actions.AI_PERSONAL);
        if (page < 0 || size < 1 || size > MAX_PAGE) {
            throw ApiException.badRequest("AI_USAGE_PAGE_INVALID", "Trang không hợp lệ (tối đa 100 dòng)");
        }
        if (from != null && to != null && from.isAfter(to)) {
            throw ApiException.badRequest("AI_USAGE_RANGE_INVALID", "Ngày bắt đầu phải trước ngày kết thúc");
        }
        long total = repository.countOwn(actor.accountId(), source, from, to);
        List<AiUsageRecord> items = total == 0 ? List.of()
                : repository.listOwn(actor.accountId(), source, from, to, page * size, size);
        Map<Source, long[]> totals = repository.totalsOwn(actor.accountId(), from, to);
        return new UsagePage(items, page, size, total, totals.getOrDefault(Source.SHARED, new long[2]),
                totals.getOrDefault(Source.PERSONAL, new long[2]), totals.getOrDefault(Source.MCP, new long[2]));
    }

    /** {@code shared}, {@code personal}, {@code mcp} là [số lượt, tổng token có số] trong kỳ. */
    public record UsagePage(List<AiUsageRecord> items, int page, int size, long total, long[] shared, long[] personal,
                            long[] mcp) {
    }
}
