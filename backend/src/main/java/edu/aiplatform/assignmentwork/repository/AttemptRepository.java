package edu.aiplatform.assignmentwork.repository;

import edu.aiplatform.assignmentwork.model.AssignmentAttempt;
import edu.aiplatform.assignmentwork.model.AttemptSnapshot;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.aiplatform.assignments.contract.DocumentModel;
import edu.aiplatform.assignments.contract.ValidationIssue;
import edu.aiplatform.assignmentwork.contract.AttemptStatus;
import edu.aiplatform.assignmentwork.contract.SubmitMode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Bảng {@code assignment_attempts}. Lưu nháp có điều kiện {@code contentVersion} (P1); nộp, tự nộp là câu lệnh có
 * điều kiện trạng thái nên chạy lại không nộp hai lần (P2).
 */
@Repository
public class AttemptRepository {

    private static final String COLUMNS = "id, assignment_id, class_id, account_id, attempt_no, snapshot, content, "
            + "content_version, status, submit_mode, late, started_at, deadline_at, last_saved_at, submitted_at, "
            + "receipt_hash, submit_warnings";
    private static final String SUMMARY_COLUMNS = "id, assignment_id, class_id, account_id, attempt_no, snapshot, "
            + "'{\"blocks\":[]}' as content, content_version, status, submit_mode, late, started_at, deadline_at, "
            + "last_saved_at, submitted_at, receipt_hash, submit_warnings";

    private final JdbcClient jdbc;
    private final ObjectMapper json;

    public AttemptRepository(JdbcClient jdbc, ObjectMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    /** Khóa theo (bài, người học) tới cuối transaction: bấm Bắt đầu hai lần không tạo hai lượt (NFR-U08-11). */
    public void lockStart(UUID assignmentId, UUID accountId) {
        jdbc.sql("select 1 from (select pg_advisory_xact_lock(hashtextextended(:key, 0))) locked")
                .param("key", "attempt-start:" + assignmentId + ":" + accountId).query(Integer.class).single();
    }

    public void insert(AssignmentAttempt a) {
        jdbc.sql("insert into assignment_attempts (" + COLUMNS + ") values (:id, :assignmentId, :classId, :accountId, "
                        + ":attemptNo, cast(:snapshot as jsonb), cast(:content as jsonb), :contentVersion, :status, "
                        + ":submitMode, :late, :startedAt, :deadlineAt, :lastSavedAt, :submittedAt, :receiptHash, "
                        + "cast(:warnings as jsonb))")
                .param("id", a.id()).param("assignmentId", a.assignmentId()).param("classId", a.classId())
                .param("accountId", a.accountId()).param("attemptNo", a.attemptNo())
                .param("snapshot", writeJson(a.snapshot())).param("content", writeJson(a.content()))
                .param("contentVersion", a.contentVersion()).param("status", a.status().name())
                .param("submitMode", a.submitMode() == null ? null : a.submitMode().name()).param("late", a.late())
                .param("startedAt", convertTimestamp(a.startedAt())).param("deadlineAt", convertTimestamp(a.deadlineAt()))
                .param("lastSavedAt", convertTimestamp(a.lastSavedAt())).param("submittedAt", convertTimestamp(a.submittedAt()))
                .param("receiptHash", a.receiptHash()).param("warnings", writeJson(a.submitWarnings()))
                .update();
    }

    public Optional<AssignmentAttempt> findAttempt(UUID id) {
        return jdbc.sql("select " + COLUMNS + " from assignment_attempts where id = :id").param("id", id)
                .query(this::map).optional();
    }

    public Optional<AssignmentAttempt> findForUpdate(UUID id) {
        return jdbc.sql("select " + COLUMNS + " from assignment_attempts where id = :id for update").param("id", id)
                .query(this::map).optional();
    }

    public Optional<AssignmentAttempt> findInProgress(UUID assignmentId, UUID accountId) {
        return jdbc.sql("select " + COLUMNS + " from assignment_attempts where assignment_id = :assignmentId "
                        + "and account_id = :accountId and status = 'IN_PROGRESS'")
                .param("assignmentId", assignmentId).param("accountId", accountId).query(this::map).optional();
    }

    public int findMaxAttemptNo(UUID assignmentId, UUID accountId) {
        return jdbc.sql("select coalesce(max(attempt_no), 0) from assignment_attempts where assignment_id = :assignmentId "
                        + "and account_id = :accountId")
                .param("assignmentId", assignmentId).param("accountId", accountId).query(Integer.class).single();
    }

    /** Lượt của một người ở một bài, không kèm nội dung, mới nhất trước. */
    public List<AssignmentAttempt> listMine(UUID assignmentId, UUID accountId) {
        return jdbc.sql("select " + SUMMARY_COLUMNS + " from assignment_attempts where assignment_id = :assignmentId "
                        + "and account_id = :accountId order by attempt_no desc")
                .param("assignmentId", assignmentId).param("accountId", accountId).query(this::map).list();
    }

    /** Lượt của một người ở nhiều bài (Student Assignments), không kèm nội dung. */
    public List<AssignmentAttempt> listMine(UUID accountId, Collection<UUID> assignmentIds) {
        if (assignmentIds.isEmpty()) {
            return List.of();
        }
        return jdbc.sql("select " + SUMMARY_COLUMNS + " from assignment_attempts where account_id = :accountId "
                        + "and assignment_id in (:ids) order by attempt_no desc")
                .param("accountId", accountId).param("ids", assignmentIds).query(this::map).list();
    }

    /** Lượt của bài ở một lớp, không kèm nội dung (U09, U14). */
    public List<AssignmentAttempt> listOfClass(UUID assignmentId, UUID classId) {
        return jdbc.sql("select " + SUMMARY_COLUMNS + " from assignment_attempts where assignment_id = :assignmentId "
                        + "and class_id = :classId order by account_id, attempt_no")
                .param("assignmentId", assignmentId).param("classId", classId).query(this::map).list();
    }

    public List<UUID> listInProgress(UUID assignmentId) {
        return jdbc.sql("select id from assignment_attempts where assignment_id = :assignmentId "
                        + "and status = 'IN_PROGRESS'")
                .param("assignmentId", assignmentId).query(UUID.class).list();
    }

    /** Lượt đang làm đã qua hạn cộng ân hạn (F7). */
    public List<UUID> listOverdue(Instant cutoff, int limit) {
        return jdbc.sql("select id from assignment_attempts where status = 'IN_PROGRESS' and deadline_at < :cutoff "
                        + "order by deadline_at limit :limit")
                .param("cutoff", convertTimestamp(cutoff)).param("limit", limit).query(UUID.class).list();
    }

    /** P1: chỉ ghi khi còn làm và {@code contentVersion} khớp; trả số dòng. */
    public int saveContent(UUID id, DocumentModel content, long expectedVersion, Instant now) {
        return jdbc.sql("update assignment_attempts set content = cast(:content as jsonb), "
                        + "content_version = content_version + 1, last_saved_at = :now where id = :id "
                        + "and status = 'IN_PROGRESS' and content_version = :version")
                .param("id", id).param("content", writeJson(content)).param("version", expectedVersion)
                .param("now", convertTimestamp(now)).update();
    }

    /** Hạn của bài được kéo dài sau khi bắt đầu lượt: lượt nhận hạn mới. */
    public int extendDeadline(UUID id, Instant deadlineAt) {
        return jdbc.sql("update assignment_attempts set deadline_at = :deadline where id = :id "
                        + "and status = 'IN_PROGRESS' and deadline_at < :deadline")
                .param("id", id).param("deadline", convertTimestamp(deadlineAt)).update();
    }

    /** P2: {@code IN_PROGRESS → SUBMITTED}; 0 dòng nghĩa là lượt đã nộp trước đó. */
    public int submit(UUID id, SubmitMode mode, boolean late, String receiptHash, Instant submittedAt,
                      List<ValidationIssue> warnings) {
        return jdbc.sql("update assignment_attempts set status = 'SUBMITTED', submit_mode = :mode, late = :late, "
                        + "receipt_hash = :hash, submitted_at = :submittedAt, submit_warnings = cast(:warnings as jsonb) "
                        + "where id = :id and status = 'IN_PROGRESS'")
                .param("id", id).param("mode", mode.name()).param("late", late).param("hash", receiptHash)
                .param("submittedAt", convertTimestamp(submittedAt)).param("warnings", writeJson(warnings)).update();
    }

    private AssignmentAttempt map(ResultSet rs, int row) throws SQLException {
        String mode = rs.getString("submit_mode");
        return new AssignmentAttempt(rs.getObject("id", UUID.class), rs.getObject("assignment_id", UUID.class),
                rs.getObject("class_id", UUID.class), rs.getObject("account_id", UUID.class), rs.getInt("attempt_no"),
                readJson(rs.getString("snapshot"), new TypeReference<AttemptSnapshot>() { }),
                readJson(rs.getString("content"), new TypeReference<DocumentModel>() { }), rs.getLong("content_version"),
                AttemptStatus.valueOf(rs.getString("status")), mode == null ? null : SubmitMode.valueOf(mode),
                rs.getBoolean("late"), readInstant(rs, "started_at"), readInstant(rs, "deadline_at"),
                readInstant(rs, "last_saved_at"), readInstant(rs, "submitted_at"), rs.getString("receipt_hash"),
                readJson(rs.getString("submit_warnings"), new TypeReference<List<ValidationIssue>>() { }));
    }

    private String writeJson(Object value) {
        try {
            return json.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Không ghi được JSON của lượt làm", e);
        }
    }

    private <T> T readJson(String value, TypeReference<T> type) {
        try {
            return json.readValue(value, type);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Không đọc được JSON của lượt làm", e);
        }
    }

    static Timestamp convertTimestamp(Instant instant) {
        return instant == null ? null : Timestamp.from(instant);
    }

    static Instant readInstant(ResultSet rs, String column) throws SQLException {
        Timestamp value = rs.getTimestamp(column);
        return value == null ? null : value.toInstant();
    }
}
