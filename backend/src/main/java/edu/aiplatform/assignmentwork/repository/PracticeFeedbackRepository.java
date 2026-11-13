package edu.aiplatform.assignmentwork.repository;

import edu.aiplatform.assignmentwork.model.PracticeFeedback;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Bảng {@code practice_feedback}; chỉ thêm và chuyển trạng thái có điều kiện, không xóa. */
@Repository
public class PracticeFeedbackRepository {

    private final JdbcClient jdbc;
    private final ObjectMapper json;

    public PracticeFeedbackRepository(JdbcClient jdbc, ObjectMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    /** Serialize the shared API/MCP daily budget and pending checks until the reservation commits. */
    public void lockAccount(UUID accountId) {
        jdbc.sql("select 1 from (select pg_advisory_xact_lock(hashtextextended(:key, 0))) locked")
                .param("key", "practice-feedback:" + accountId).query(Integer.class).single();
    }

    public void insert(PracticeFeedback f) {
        jdbc.sql("insert into practice_feedback (id, attempt_id, account_id, origin, task_id, request_ref, status, "
                        + "submission_hash, feedback, provenance, safe_error, created_at, completed_at) values (:id, "
                        + ":attemptId, :accountId, :origin, :taskId, :requestRef, :status, :hash, cast(:feedback as jsonb), "
                        + "cast(:provenance as jsonb), :safeError, :createdAt, :completedAt)")
                .param("id", f.id()).param("attemptId", f.attemptId()).param("accountId", f.accountId())
                .param("origin", f.origin().name()).param("taskId", f.taskId()).param("requestRef", f.requestRef())
                .param("status", f.status().name()).param("hash", f.submissionHash())
                .param("feedback", f.feedback() == null ? null : write(f.feedback()))
                .param("provenance", write(f.provenance())).param("safeError", f.safeError())
                .param("createdAt", AttemptRepository.ts(f.createdAt()))
                .param("completedAt", AttemptRepository.ts(f.completedAt()))
                .update();
    }

    public Optional<PracticeFeedback> find(UUID id) {
        return jdbc.sql("select * from practice_feedback where id = :id").param("id", id).query(this::map).optional();
    }

    public Optional<PracticeFeedback> findByRequest(String requestRef) {
        return jdbc.sql("select * from practice_feedback where request_ref = :ref").param("ref", requestRef)
                .query(this::map).optional();
    }

    public List<PracticeFeedback> listByAttempt(UUID attemptId) {
        return jdbc.sql("select * from practice_feedback where attempt_id = :id order by created_at desc")
                .param("id", attemptId).query(this::map).list();
    }

    public boolean hasPending(UUID attemptId) {
        return jdbc.sql("select count(*) from practice_feedback where attempt_id = :id and status = 'PENDING'")
                .param("id", attemptId).query(Long.class).single() > 0;
    }

    /** Số yêu cầu của một người từ {@code since} (web và MCP cộng chung, BR-U08-44). */
    public long countSince(UUID accountId, Instant since) {
        return jdbc.sql("select count(*) from practice_feedback where account_id = :id and created_at >= :since")
                .param("id", accountId).param("since", AttemptRepository.ts(since)).query(Long.class).single();
    }

    /** Số phản hồi READY theo lượt, cho danh sách lượt. */
    public Map<UUID, Long> readyCounts(Collection<UUID> attemptIds) {
        if (attemptIds.isEmpty()) {
            return Map.of();
        }
        return jdbc.sql("select attempt_id, count(*) as n from practice_feedback where attempt_id in (:ids) "
                        + "and status = 'READY' group by attempt_id")
                .param("ids", attemptIds)
                .query((rs, row) -> Map.entry(rs.getObject("attempt_id", UUID.class), rs.getLong("n"))).list()
                .stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public int attachTask(UUID id, UUID taskId, JsonNode provenance) {
        return jdbc.sql("update practice_feedback set task_id = :taskId, provenance = cast(:provenance as jsonb) "
                        + "where id = :id and task_id is null")
                .param("id", id).param("taskId", taskId).param("provenance", write(provenance)).update();
    }

    public int markReady(UUID id, JsonNode feedback, JsonNode provenance, Instant now) {
        return jdbc.sql("update practice_feedback set status = 'READY', feedback = cast(:feedback as jsonb), "
                        + "provenance = cast(:provenance as jsonb), completed_at = :now where id = :id "
                        + "and status = 'PENDING'")
                .param("id", id).param("feedback", write(feedback)).param("provenance", write(provenance))
                .param("now", AttemptRepository.ts(now)).update();
    }

    public int markFailed(UUID id, String safeError, Instant now) {
        return jdbc.sql("update practice_feedback set status = 'FAILED', safe_error = :error, completed_at = :now "
                        + "where id = :id and status = 'PENDING'")
                .param("id", id).param("error", safeError).param("now", AttemptRepository.ts(now)).update();
    }

    private PracticeFeedback map(ResultSet rs, int row) throws SQLException {
        String feedback = rs.getString("feedback");
        return new PracticeFeedback(rs.getObject("id", UUID.class), rs.getObject("attempt_id", UUID.class),
                rs.getObject("account_id", UUID.class), PracticeFeedback.Origin.valueOf(rs.getString("origin")),
                rs.getObject("task_id", UUID.class), rs.getString("request_ref"),
                PracticeFeedback.Status.valueOf(rs.getString("status")), rs.getString("submission_hash"),
                feedback == null ? null : read(feedback), read(rs.getString("provenance")), rs.getString("safe_error"),
                AttemptRepository.instant(rs, "created_at"), AttemptRepository.instant(rs, "completed_at"));
    }

    private String write(JsonNode value) {
        try {
            return json.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Không ghi được JSON phản hồi", e);
        }
    }

    private JsonNode read(String value) {
        try {
            return json.readTree(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Không đọc được JSON phản hồi", e);
        }
    }
}
