package edu.aiplatform.audit.domain;

import edu.aiplatform.audit.contract.AuditFilter;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Bảng {@code audit_logs}; chỉ dùng trong U02. Chỉ có thêm và đọc, không có sửa hay xóa (BR-U02-01).
 */
@Repository
public class AuditLogRepository {

    private static final String COLUMNS = "id, actor_id, action, object_type, object_id, result, reason, "
            + "details::text as details, occurred_at";

    private final JdbcClient jdbc;

    public AuditLogRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /** Ghi một dòng; {@code id} đã có thì bỏ qua (BR-U02-03). Trả số dòng thêm được (0 hoặc 1). */
    public int insertIfAbsent(AuditRow row) {
        return jdbc.sql("insert into audit_logs (id, actor_id, action, object_type, object_id, result, reason, details, "
                        + "occurred_at) values (:id, :actorId, :action, :objectType, :objectId, :result, :reason, "
                        + "cast(:details as jsonb), :occurredAt) on conflict (id) do nothing")
                .param("id", row.id()).param("actorId", row.actorId()).param("action", row.action())
                .param("objectType", row.objectType()).param("objectId", row.objectId())
                .param("result", row.result()).param("reason", row.reason()).param("details", row.detailsJson())
                .param("occurredAt", Timestamp.from(row.occurredAt())).update();
    }

    public long count(AuditFilter filter) {
        Where where = Where.of(filter);
        return jdbc.sql("select count(*) from audit_logs" + where.sql()).params(where.params())
                .query(Long.class).single();
    }

    /** Số bản ghi SUCCESS của {@code action} trong {@code [from, to)}, lọc thêm một khóa của {@code details} nếu có. */
    public long countSucceeded(String action, Instant from, Instant to, String detailKey, String detailValue) {
        String sql = "select count(*) from audit_logs where action = :action and result = 'SUCCESS' "
                + "and occurred_at >= :from and occurred_at < :to"
                + (detailKey == null ? "" : " and details ->> :detailKey = :detailValue");
        var spec = jdbc.sql(sql).param("action", action).param("from", Timestamp.from(from))
                .param("to", Timestamp.from(to));
        if (detailKey != null) {
            spec = spec.param("detailKey", detailKey).param("detailValue", detailValue);
        }
        return spec.query(Long.class).single();
    }

    /** Bản ghi bị từ chối hoặc thuộc {@code actions} từ {@code from}, mới nhất trước. */
    public List<AuditRow> findSecurity(Instant from, Collection<String> actions, int limit) {
        return jdbc.sql("select " + COLUMNS + " from audit_logs where occurred_at >= :from "
                        + "and (result = 'DENIED' or action in (:actions)) order by occurred_at desc, id desc limit :limit")
                .param("from", Timestamp.from(from)).param("actions", actions).param("limit", limit)
                .query(AuditLogRepository::map).list();
    }

    /** Mới nhất trước; cùng thời điểm thì theo {@code id} để thứ tự trang ổn định. */
    public List<AuditRow> find(AuditFilter filter, long offset, int limit) {
        Where where = Where.of(filter);
        Map<String, Object> params = new LinkedHashMap<>(where.params());
        params.put("offset", offset);
        params.put("limit", limit);
        return jdbc.sql("select " + COLUMNS + " from audit_logs" + where.sql()
                        + " order by occurred_at desc, id desc offset :offset limit :limit")
                .params(params).query(AuditLogRepository::map).list();
    }

    private static AuditRow map(ResultSet rs, int rowNum) throws SQLException {
        return new AuditRow(rs.getObject("id", UUID.class), rs.getObject("actor_id", UUID.class),
                rs.getString("action"), rs.getString("object_type"), rs.getString("object_id"),
                rs.getString("result"), rs.getString("reason"), rs.getString("details"),
                rs.getTimestamp("occurred_at").toInstant());
    }

    /** Điều kiện WHERE dựng từ bộ lọc; trường rỗng thì không lọc. Tách riêng để kiểm thử không cần database. */
    record Where(String sql, Map<String, Object> params) {

        static Where of(AuditFilter filter) {
            List<String> conditions = new ArrayList<>();
            Map<String, Object> params = new LinkedHashMap<>();
            if (filter != null) {
                add(conditions, params, "actor_id = :actorId", "actorId", filter.actorId());
                if (filter.actorIds() != null) {
                    // Không ai khớp phần email đã gõ thì không dòng nào khớp.
                    if (filter.actorIds().isEmpty()) {
                        conditions.add("1 = 0");
                    } else {
                        add(conditions, params, "actor_id in (:actorIds)", "actorIds", filter.actorIds());
                    }
                }
                add(conditions, params, "action = :action", "action", filter.action());
                add(conditions, params, "object_type = :objectType", "objectType", filter.objectType());
                add(conditions, params, "object_id = :objectId", "objectId", filter.objectId());
                add(conditions, params, "result = :result", "result",
                        filter.result() == null ? null : filter.result().name());
                add(conditions, params, "occurred_at >= :from", "from",
                        filter.from() == null ? null : Timestamp.from(filter.from()));
                add(conditions, params, "occurred_at <= :to", "to",
                        filter.to() == null ? null : Timestamp.from(filter.to()));
            }
            String sql = conditions.isEmpty() ? "" : " where " + String.join(" and ", conditions);
            return new Where(sql, params);
        }

        private static void add(List<String> conditions, Map<String, Object> params, String condition, String name,
                                Object value) {
            if (value != null) {
                conditions.add(condition);
                params.put(name, value);
            }
        }
    }
}
