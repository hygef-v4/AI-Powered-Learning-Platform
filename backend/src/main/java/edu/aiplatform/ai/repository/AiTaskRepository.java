package edu.aiplatform.ai.repository;

import edu.aiplatform.ai.model.AiTask;

import edu.aiplatform.ai.contract.AiProvider;
import edu.aiplatform.ai.contract.AiSource;
import edu.aiplatform.ai.contract.AiTaskState;
import edu.aiplatform.ai.contract.ApplyOutcome;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Bảng {@code ai_tasks}; chỉ dùng trong U06. {@code request_ref} duy nhất (NFR-U06-11). */
@Repository
public class AiTaskRepository {

    private static final String COLUMNS = "id, request_ref, fingerprint, actor_id, source, connection_id, provider, model, "
            + "task_type, target_ref, input_version, state, input_payload::text as input_payload, "
            + "output_schema::text as output_schema, result_payload::text as result_payload, max_output_tokens, "
            + "reserved_tokens, quota_day, dispatched_at, provider_request_id, apply_result, handler_done, safe_error, "
            + "created_at, finished_at, version";

    private final JdbcClient jdbc;

    public AiTaskRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public void insert(AiTask t) {
        jdbc.sql("insert into ai_tasks (id, request_ref, fingerprint, actor_id, source, connection_id, provider, model, "
                        + "task_type, target_ref, input_version, state, input_payload, output_schema, result_payload, "
                        + "max_output_tokens, reserved_tokens, quota_day, dispatched_at, provider_request_id, apply_result, "
                        + "handler_done, safe_error, created_at, finished_at, version) values (:id, :requestRef, "
                        + ":fingerprint, :actorId, :source, :connectionId, :provider, :model, :taskType, :targetRef, "
                        + ":inputVersion, :state, cast(:input as jsonb), cast(:schema as jsonb), cast(:result as jsonb), "
                        + ":maxOutput, :reserved, :quotaDay, :dispatchedAt, :providerRequestId, :applyResult, "
                        + ":handlerDone, :safeError, :createdAt, :finishedAt, 0)")
                .params(params(t)).update();
    }

    public boolean update(AiTask t) {
        return jdbc.sql("update ai_tasks set state = :state, input_payload = cast(:input as jsonb), "
                        + "output_schema = cast(:schema as jsonb), result_payload = cast(:result as jsonb), "
                        + "dispatched_at = :dispatchedAt, provider_request_id = :providerRequestId, "
                        + "apply_result = :applyResult, handler_done = :handlerDone, safe_error = :safeError, "
                        + "finished_at = :finishedAt, version = version + 1 where id = :id and version = :version")
                .params(params(t)).param("version", t.version()).update() == 1;
    }

    public Optional<AiTask> findById(UUID id) {
        return jdbc.sql("select " + COLUMNS + " from ai_tasks where id = :id").param("id", id)
                .query(AiTaskRepository::map).optional();
    }

    /** Khóa dòng task để worker và người hủy không chạy chồng nhau. */
    public Optional<AiTask> lock(UUID id) {
        return jdbc.sql("select " + COLUMNS + " from ai_tasks where id = :id for update").param("id", id)
                .query(AiTaskRepository::map).optional();
    }

    public Optional<AiTask> findByRequestRef(String requestRef) {
        return jdbc.sql("select " + COLUMNS + " from ai_tasks where request_ref = :ref").param("ref", requestRef)
                .query(AiTaskRepository::map).optional();
    }

    /** Khóa theo người gửi tới cuối transaction: hai yêu cầu đến cùng lúc không cùng lọt qua giới hạn 3 tác vụ. */
    public void lockActor(UUID actorId) {
        jdbc.sql("select 1 from (select pg_advisory_xact_lock(hashtextextended(:key, 0))) locked")
                .param("key", "ai-task-submit:" + actorId).query(Integer.class).single();
    }

    public int countActive(UUID actorId) {
        return jdbc.sql("select count(*) from ai_tasks where actor_id = :actorId and state in ('QUEUED', 'RUNNING')")
                .param("actorId", actorId).query(Integer.class).single();
    }

    public List<AiTask> listByStateBefore(AiTaskState state, Instant before) {
        return jdbc.sql("select " + COLUMNS + " from ai_tasks where state = :state and created_at < :before "
                        + "order by created_at limit 200")
                .param("state", state.name()).param("before", Timestamp.from(before)).query(AiTaskRepository::map).list();
    }

    /** Task đã kết thúc nhưng chưa chuyển xong cho handler (P3: lượt sau chỉ gọi handler). */
    public List<AiTask> listUnhandled(Instant before) {
        return jdbc.sql("select " + COLUMNS + " from ai_tasks where state in ('READY', 'FAILED', 'UNKNOWN') "
                        + "and handler_done = false and finished_at < :before order by finished_at limit 200")
                .param("before", Timestamp.from(before)).query(AiTaskRepository::map).list();
    }

    private static Map<String, Object> params(AiTask t) {
        Map<String, Object> p = new HashMap<>();
        p.put("id", t.id());
        p.put("requestRef", t.requestRef());
        p.put("fingerprint", t.fingerprint());
        p.put("actorId", t.actorId());
        p.put("source", t.source().name());
        p.put("connectionId", t.connectionId());
        p.put("provider", t.provider().name());
        p.put("model", t.model());
        p.put("taskType", t.taskType());
        p.put("targetRef", t.targetRef());
        p.put("inputVersion", t.inputVersion());
        p.put("state", t.state().name());
        p.put("input", t.inputPayload());
        p.put("schema", t.outputSchema());
        p.put("result", t.resultPayload());
        p.put("maxOutput", t.maxOutputTokens());
        p.put("reserved", t.reservedTokens());
        p.put("quotaDay", t.quotaDay() == null ? null : Date.valueOf(t.quotaDay()));
        p.put("dispatchedAt", ts(t.dispatchedAt()));
        p.put("providerRequestId", t.providerRequestId());
        p.put("applyResult", t.applyResult() == null ? null : t.applyResult().name());
        p.put("handlerDone", t.handlerDone());
        p.put("safeError", t.safeError());
        p.put("createdAt", ts(t.createdAt()));
        p.put("finishedAt", ts(t.finishedAt()));
        return p;
    }

    private static AiTask map(ResultSet rs, int n) throws SQLException {
        Date quotaDay = rs.getDate("quota_day");
        String apply = rs.getString("apply_result");
        return new AiTask(rs.getObject("id", UUID.class), rs.getString("request_ref"), rs.getString("fingerprint"),
                rs.getObject("actor_id", UUID.class), AiSource.valueOf(rs.getString("source")),
                rs.getObject("connection_id", UUID.class), AiProvider.valueOf(rs.getString("provider")),
                rs.getString("model"), rs.getString("task_type"), rs.getString("target_ref"), rs.getLong("input_version"),
                AiTaskState.valueOf(rs.getString("state")), rs.getString("input_payload"), rs.getString("output_schema"),
                rs.getString("result_payload"), rs.getInt("max_output_tokens"), rs.getObject("reserved_tokens", Long.class),
                quotaDay == null ? null : quotaDay.toLocalDate(), instant(rs.getTimestamp("dispatched_at")),
                rs.getString("provider_request_id"), apply == null ? null : ApplyOutcome.valueOf(apply),
                rs.getBoolean("handler_done"), rs.getString("safe_error"), instant(rs.getTimestamp("created_at")),
                instant(rs.getTimestamp("finished_at")), rs.getLong("version"));
    }

    private static Timestamp ts(Instant value) {
        return value == null ? null : Timestamp.from(value);
    }

    private static Instant instant(Timestamp value) {
        return value == null ? null : value.toInstant();
    }
}
