package edu.aiplatform.assignments.repository;

import edu.aiplatform.assignments.model.AssignmentAiDraft;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Bảng {@code assignment_ai_drafts}; chuyển trạng thái có điều kiện để kết quả đến muộn không ghi đè quyết định. */
@Repository
public class AiDraftRepository {

    private final JdbcClient jdbc;
    private final ObjectMapper json;

    public AiDraftRepository(JdbcClient jdbc, ObjectMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    public void insert(AssignmentAiDraft d) {
        jdbc.sql("insert into assignment_ai_drafts (id, assignment_id, origin, task_id, request_ref, status, proposal, "
                        + "provenance, base_version, safe_error, created_by, created_at, decided_at) values (:id, "
                        + ":assignmentId, :origin, :taskId, :requestRef, :status, cast(:proposal as jsonb), "
                        + "cast(:provenance as jsonb), :baseVersion, :safeError, :createdBy, :createdAt, :decidedAt)")
                .param("id", d.id()).param("assignmentId", d.assignmentId()).param("origin", d.origin().name())
                .param("taskId", d.taskId()).param("requestRef", d.requestRef()).param("status", d.status().name())
                .param("proposal", d.proposal() == null ? null : writeJson(d.proposal()))
                .param("provenance", writeJson(d.provenance())).param("baseVersion", d.baseVersion())
                .param("safeError", d.safeError()).param("createdBy", d.createdBy())
                .param("createdAt", AssignmentRepository.convertTimestamp(d.createdAt()))
                .param("decidedAt", AssignmentRepository.convertTimestamp(d.decidedAt()))
                .update();
    }

    public Optional<AssignmentAiDraft> findDraft(UUID id) {
        return jdbc.sql("select * from assignment_ai_drafts where id = :id").param("id", id).query(this::map).optional();
    }

    public Optional<AssignmentAiDraft> findByRequest(UUID assignmentId, String requestRef) {
        return jdbc.sql("select * from assignment_ai_drafts where assignment_id = :id and request_ref = :ref")
                .param("id", assignmentId).param("ref", requestRef).query(this::map).optional();
    }

    public int attachTask(UUID id, UUID taskId, JsonNode provenance) {
        return jdbc.sql("update assignment_ai_drafts set task_id = :taskId, provenance = cast(:provenance as jsonb) "
                        + "where id = :id and task_id is null")
                .param("id", id).param("taskId", taskId).param("provenance", writeJson(provenance)).update();
    }

    public int updatePendingProvenance(UUID id, JsonNode provenance) {
        return jdbc.sql("update assignment_ai_drafts set provenance = cast(:provenance as jsonb) "
                        + "where id = :id and status = 'PENDING' and task_id is null")
                .param("id", id).param("provenance", writeJson(provenance)).update();
    }

    public int markReady(UUID id, JsonNode proposal, JsonNode provenance) {
        return jdbc.sql("update assignment_ai_drafts set status = 'READY', proposal = cast(:proposal as jsonb), "
                        + "provenance = cast(:provenance as jsonb) where id = :id and status = 'PENDING'")
                .param("id", id).param("proposal", writeJson(proposal)).param("provenance", writeJson(provenance)).update();
    }

    public int markFailed(UUID id, String safeError, Instant now) {
        return jdbc.sql("update assignment_ai_drafts set status = 'FAILED', safe_error = :error, decided_at = :now "
                        + "where id = :id and status = 'PENDING'")
                .param("id", id).param("error", safeError).param("now", AssignmentRepository.convertTimestamp(now)).update();
    }

    public int markApplied(UUID id, Instant now) {
        return jdbc.sql("update assignment_ai_drafts set status = 'APPLIED', decided_at = :now "
                        + "where id = :id and status = 'READY'")
                .param("id", id).param("now", AssignmentRepository.convertTimestamp(now)).update();
    }

    public int markDiscarded(UUID id, Instant now) {
        return jdbc.sql("update assignment_ai_drafts set status = 'DISCARDED', decided_at = :now "
                        + "where id = :id and status in ('PENDING', 'READY')")
                .param("id", id).param("now", AssignmentRepository.convertTimestamp(now)).update();
    }

    private AssignmentAiDraft map(ResultSet rs, int row) throws SQLException {
        String proposal = rs.getString("proposal");
        return new AssignmentAiDraft(rs.getObject("id", UUID.class), rs.getObject("assignment_id", UUID.class),
                AssignmentAiDraft.Origin.valueOf(rs.getString("origin")), rs.getObject("task_id", UUID.class),
                rs.getString("request_ref"), AssignmentAiDraft.Status.valueOf(rs.getString("status")),
                proposal == null ? null : parseJson(proposal), parseJson(rs.getString("provenance")), rs.getLong("base_version"),
                rs.getString("safe_error"), rs.getObject("created_by", UUID.class),
                AssignmentRepository.readInstant(rs, "created_at"), AssignmentRepository.readInstant(rs, "decided_at"));
    }

    private String writeJson(JsonNode value) {
        try {
            return json.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Không ghi được JSON của đề xuất", e);
        }
    }

    private JsonNode parseJson(String value) {
        try {
            return json.readTree(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Không đọc được JSON của đề xuất", e);
        }
    }
}
