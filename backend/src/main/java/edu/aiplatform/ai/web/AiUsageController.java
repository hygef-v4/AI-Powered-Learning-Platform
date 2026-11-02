package edu.aiplatform.ai.web;

import edu.aiplatform.ai.application.AiSourceService;
import edu.aiplatform.ai.application.AiSourceService.SourceOption;
import edu.aiplatform.ai.application.AiTaskService;
import edu.aiplatform.ai.application.AiUsageService;
import edu.aiplatform.ai.application.AiUsageService.UsagePage;
import edu.aiplatform.ai.contract.AiProvider;
import edu.aiplatform.ai.contract.AiTaskView;
import edu.aiplatform.ai.contract.UsageMeasurement;
import edu.aiplatform.ai.domain.AiUsageRecord;
import edu.aiplatform.ai.domain.AiUsageRecord.Source;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.shared.error.ApiException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** UC 33 và nguồn AI cho U07–U10: lịch sử của chính mình, nguồn dùng được, trạng thái và hủy task. */
@RestController
public class AiUsageController {

    private final AiUsageService usage;
    private final AiSourceService sources;
    private final AiTaskService tasks;

    public AiUsageController(AiUsageService usage, AiSourceService sources, AiTaskService tasks) {
        this.usage = usage;
        this.sources = sources;
        this.tasks = tasks;
    }

    /** Một dòng lịch sử; token rỗng là "không có số liệu", không đổi thành 0 (BR-U06-40). */
    public record UsageItem(UUID id, Instant createdAt, Source source, AiProvider provider, String model,
                            String taskType, String toolName, String outcome, Long inputTokens, Long outputTokens,
                            Long cachedTokens, Long reasoningTokens, UsageMeasurement provenance) {

        static UsageItem of(AiUsageRecord r) {
            return new UsageItem(r.id(), r.createdAt(), r.source(), r.provider(), r.model(), r.taskType(), r.toolName(),
                    r.outcome(), r.inputTokens(), r.outputTokens(), r.cachedTokens(), r.reasoningTokens(),
                    r.provenance());
        }
    }

    public record Totals(long requests, long tokens) {

        static Totals of(long[] values) {
            return new Totals(values[0], values[1]);
        }
    }

    public record UsageResponse(List<UsageItem> items, int page, int size, long total, Totals shared, Totals personal,
                                Totals mcp) {
    }

    @GetMapping("/api/v1/me/ai-usage")
    public UsageResponse history(@AuthenticationPrincipal Actor actor,
                                 @RequestParam(required = false) String source,
                                 @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
                                 @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
                                 @RequestParam(defaultValue = "0") int page,
                                 @RequestParam(defaultValue = "50") int size) {
        UsagePage result = usage.listOwn(actor, source(source), from, to, page, size);
        return new UsageResponse(result.items().stream().map(UsageItem::of).toList(), result.page(), result.size(),
                result.total(), Totals.of(result.shared()), Totals.of(result.personal()), Totals.of(result.mcp()));
    }

    @GetMapping("/api/v1/ai/sources")
    public List<SourceOption> sources(@AuthenticationPrincipal Actor actor) {
        return sources.sources(actor);
    }

    @GetMapping("/api/v1/ai/tasks/{id}")
    public AiTaskView task(@AuthenticationPrincipal Actor actor, @PathVariable UUID id) {
        return tasks.getOwn(actor, id);
    }

    @PostMapping("/api/v1/ai/tasks/{id}:cancel")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(@AuthenticationPrincipal Actor actor, @PathVariable UUID id) {
        tasks.cancel(actor, id);
    }

    private static Source source(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Source.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw ApiException.badRequest("AI_USAGE_SOURCE_INVALID", "Nguồn không hợp lệ");
        }
    }
}
