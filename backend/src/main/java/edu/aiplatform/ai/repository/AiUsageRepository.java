package edu.aiplatform.ai.repository;

import edu.aiplatform.ai.model.AiUsageRecord;

import edu.aiplatform.ai.contract.AiProvider;
import edu.aiplatform.ai.contract.AiUsageSummary;
import edu.aiplatform.ai.contract.UsageMeasurement;
import edu.aiplatform.ai.model.AiUsageRecord.Source;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Bảng {@code ai_usage_records}, chỉ thêm và đọc; chỉ dùng trong U06. */
@Repository
public class AiUsageRepository {

    private static final String COLUMNS = "id, account_id, source, task_id, mcp_credential_id, provider, model, task_type, "
            + "tool_name, outcome, input_tokens, output_tokens, cached_tokens, reasoning_tokens, provenance, created_at";

    private final JdbcClient jdbc;

    public AiUsageRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public void insert(AiUsageRecord r) {
        Map<String, Object> p = new HashMap<>();
        p.put("id", r.id());
        p.put("accountId", r.accountId());
        p.put("source", r.source().name());
        p.put("taskId", r.taskId());
        p.put("credentialId", r.mcpCredentialId());
        p.put("provider", r.provider() == null ? null : r.provider().name());
        p.put("model", r.model());
        p.put("taskType", r.taskType());
        p.put("toolName", r.toolName());
        p.put("outcome", r.outcome());
        p.put("input", r.inputTokens());
        p.put("output", r.outputTokens());
        p.put("cached", r.cachedTokens());
        p.put("reasoning", r.reasoningTokens());
        p.put("provenance", r.provenance().name());
        p.put("createdAt", Timestamp.from(r.createdAt()));
        jdbc.sql("insert into ai_usage_records (" + COLUMNS + ") values (:id, :accountId, :source, :taskId, :credentialId, "
                        + ":provider, :model, :taskType, :toolName, :outcome, :input, :output, :cached, :reasoning, "
                        + ":provenance, :createdAt)")
                .params(p).update();
    }

    /** Lịch sử của chính một người, mới nhất trước (BR-U06-40). */
    public List<AiUsageRecord> listOwn(UUID accountId, Source source, Instant from, Instant to, int offset, int limit) {
        Map<String, Object> p = new HashMap<>();
        String where = where(accountId, source, from, to, p);
        p.put("offset", offset);
        p.put("limit", limit);
        return jdbc.sql("select " + COLUMNS + " from ai_usage_records" + where
                        + " order by created_at desc, id desc offset :offset limit :limit")
                .params(p).query(AiUsageRepository::map).list();
    }

    public long countOwn(UUID accountId, Source source, Instant from, Instant to) {
        Map<String, Object> p = new HashMap<>();
        return jdbc.sql("select count(*) from ai_usage_records" + where(accountId, source, from, to, p)).params(p)
                .query(Long.class).single();
    }

    /** Tổng số lượt và token theo nguồn của một người trong kỳ (thẻ trên AI Usage History). */
    public Map<Source, long[]> totalsOwn(UUID accountId, Instant from, Instant to) {
        Map<Source, long[]> totals = new HashMap<>();
        Map<String, Object> p = new HashMap<>();
        jdbc.sql("select source, count(*) as n, coalesce(sum(input_tokens), 0) + coalesce(sum(output_tokens), 0) as tokens "
                        + "from ai_usage_records" + where(accountId, null, from, to, p) + " group by source")
                .params(p).query((rs, n) -> totals.put(Source.valueOf(rs.getString("source")),
                        new long[] {rs.getLong("n"), rs.getLong("tokens")}))
                .list();
        return totals;
    }

    /** Token của một task (lấy dòng usage mới nhất của task) cho {@code AiTaskView}. */
    public Optional<AiUsageSummary> summaryOfTask(UUID taskId) {
        return jdbc.sql("select input_tokens, output_tokens, provenance from ai_usage_records where task_id = :taskId "
                        + "order by created_at desc limit 1")
                .param("taskId", taskId)
                .query((rs, n) -> new AiUsageSummary(rs.getObject("input_tokens", Long.class),
                        rs.getObject("output_tokens", Long.class), UsageMeasurement.valueOf(rs.getString("provenance"))))
                .optional();
    }

    /** Tổng hợp API chung cho Admin (BR-U06-41): theo giảng viên, ngày, provider và model; không có nội dung. */
    public List<SharedRow> sharedSummary(Instant from, Instant to) {
        return jdbc.sql("select account_id, (created_at at time zone 'Asia/Ho_Chi_Minh')::date as day, provider, model, "
                        + "count(*) as requests, coalesce(sum(input_tokens), 0) + coalesce(sum(output_tokens), 0) as tokens "
                        + "from ai_usage_records where source = 'SHARED' and created_at >= :from and created_at < :to "
                        + "group by account_id, day, provider, model order by day")
                .param("from", Timestamp.from(from)).param("to", Timestamp.from(to))
                .query((rs, n) -> new SharedRow(rs.getObject("account_id", UUID.class), rs.getDate("day").toLocalDate(),
                        rs.getString("provider") == null ? null : AiProvider.valueOf(rs.getString("provider")),
                        rs.getString("model"), rs.getLong("requests"), rs.getLong("tokens")))
                .list();
    }

    public record SharedRow(UUID accountId, LocalDate day, AiProvider provider, String model, long requests,
                            long tokens) {
    }

    private static String where(UUID accountId, Source source, Instant from, Instant to, Map<String, Object> p) {
        List<String> conditions = new ArrayList<>();
        conditions.add("account_id = :accountId");
        p.put("accountId", accountId);
        if (source != null) {
            conditions.add("source = :source");
            p.put("source", source.name());
        }
        if (from != null) {
            conditions.add("created_at >= :from");
            p.put("from", Timestamp.from(from));
        }
        if (to != null) {
            conditions.add("created_at < :to");
            p.put("to", Timestamp.from(to));
        }
        return " where " + String.join(" and ", conditions);
    }

    private static AiUsageRecord map(ResultSet rs, int n) throws SQLException {
        String provider = rs.getString("provider");
        return new AiUsageRecord(rs.getObject("id", UUID.class), rs.getObject("account_id", UUID.class),
                Source.valueOf(rs.getString("source")), rs.getObject("task_id", UUID.class),
                rs.getObject("mcp_credential_id", UUID.class), provider == null ? null : AiProvider.valueOf(provider),
                rs.getString("model"), rs.getString("task_type"), rs.getString("tool_name"), rs.getString("outcome"),
                rs.getObject("input_tokens", Long.class), rs.getObject("output_tokens", Long.class),
                rs.getObject("cached_tokens", Long.class), rs.getObject("reasoning_tokens", Long.class),
                UsageMeasurement.valueOf(rs.getString("provenance")), rs.getTimestamp("created_at").toInstant());
    }
}
