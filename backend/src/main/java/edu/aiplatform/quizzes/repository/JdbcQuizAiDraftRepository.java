package edu.aiplatform.quizzes.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.aiplatform.quizzes.model.DraftOrigin;
import edu.aiplatform.quizzes.model.DraftQuestion;
import edu.aiplatform.quizzes.model.DraftStatus;
import edu.aiplatform.quizzes.model.DraftTarget;
import edu.aiplatform.quizzes.repository.IQuizAiDraftRepository;
import edu.aiplatform.quizzes.model.QuizAiDraft;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** {@code quiz_ai_drafts}: đề xuất và provenance lưu {@code jsonb}; đổi trạng thái có điều kiện (P4). */
@Repository
public class JdbcQuizAiDraftRepository implements IQuizAiDraftRepository {

    private static final String COLUMNS = """
            id, target, quiz_id, subject_id, origin, task_id, status, proposal, provenance, base_version, created_by,
            created_at, decided_at""";
    private static final String OPEN = "status in ('PENDING', 'READY', 'FAILED')";
    private static final TypeReference<List<DraftQuestion>> PROPOSAL = new TypeReference<>() { };
    private static final TypeReference<Map<String, Object>> PROVENANCE = new TypeReference<>() { };

    private final JdbcClient jdbc;
    private final ObjectMapper json;

    public JdbcQuizAiDraftRepository(JdbcClient jdbc, ObjectMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    @Override
    public void insert(QuizAiDraft d) {
        jdbc.sql("""
                        insert into quiz_ai_drafts (id, target, quiz_id, subject_id, origin, task_id, status, proposal,
                            provenance, base_version, created_by, created_at, decided_at)
                        values (:id, :target, :quizId, :subjectId, :origin, :taskId, :status, cast(:proposal as jsonb),
                            cast(:provenance as jsonb), :baseVersion, :createdBy, :createdAt, :decidedAt)
                        """)
                .param("id", d.id())
                .param("target", d.target().name())
                .param("quizId", d.quizId())
                .param("subjectId", d.subjectId())
                .param("origin", d.origin().name())
                .param("taskId", d.taskId())
                .param("status", d.status().name())
                .param("proposal", write(d.proposal()))
                .param("provenance", write(d.provenance()))
                .param("baseVersion", d.baseVersion())
                .param("createdBy", d.createdBy())
                .param("createdAt", timestamp(d.createdAt()))
                .param("decidedAt", timestamp(d.decidedAt()))
                .update();
    }

    @Override
    public Optional<QuizAiDraft> find(UUID id) {
        return jdbc.sql("select " + COLUMNS + " from quiz_ai_drafts where id = :id")
                .param("id", id).query(this::map).optional();
    }

    @Override
    public Optional<QuizAiDraft> findByRequest(UUID createdBy, String requestRef) {
        return jdbc.sql("select " + COLUMNS + " from quiz_ai_drafts where created_by = :createdBy"
                        + " and provenance ->> 'requestRef' = :requestRef order by created_at limit 1")
                .param("createdBy", createdBy).param("requestRef", requestRef).query(this::map).optional();
    }

    @Override
    public List<QuizAiDraft> listOpenForQuiz(UUID quizId) {
        return jdbc.sql("select " + COLUMNS + " from quiz_ai_drafts where quiz_id = :quizId and " + OPEN
                        + " order by created_at desc")
                .param("quizId", quizId).query(this::map).list();
    }

    @Override
    public List<QuizAiDraft> listOpenForBank(UUID subjectId) {
        return jdbc.sql("select " + COLUMNS + " from quiz_ai_drafts where subject_id = :subjectId and target = 'BANK'"
                        + " and " + OPEN + " order by created_at desc")
                .param("subjectId", subjectId).query(this::map).list();
    }

    @Override
    public boolean transition(QuizAiDraft next, DraftStatus expected) {
        return jdbc.sql("""
                        update quiz_ai_drafts set status = :status, task_id = coalesce(:taskId, task_id),
                            proposal = cast(:proposal as jsonb), provenance = cast(:provenance as jsonb),
                            decided_at = :decidedAt
                        where id = :id and status = :expected
                        """)
                .param("status", next.status().name())
                .param("taskId", next.taskId())
                .param("proposal", write(next.proposal()))
                .param("provenance", write(next.provenance()))
                .param("decidedAt", timestamp(next.decidedAt()))
                .param("id", next.id())
                .param("expected", expected.name())
                .update() == 1;
    }

    @Override
    public void attachTask(UUID id, UUID taskId) {
        jdbc.sql("update quiz_ai_drafts set task_id = :taskId where id = :id and task_id is null")
                .param("taskId", taskId).param("id", id).update();
    }

    private QuizAiDraft map(ResultSet rs, int row) throws SQLException {
        Long baseVersion = rs.getObject("base_version", Long.class);
        return new QuizAiDraft(rs.getObject("id", UUID.class), DraftTarget.valueOf(rs.getString("target")),
                rs.getObject("quiz_id", UUID.class), rs.getObject("subject_id", UUID.class),
                DraftOrigin.valueOf(rs.getString("origin")), rs.getObject("task_id", UUID.class),
                DraftStatus.valueOf(rs.getString("status")), read(rs.getString("proposal"), PROPOSAL),
                read(rs.getString("provenance"), PROVENANCE), baseVersion,
                rs.getObject("created_by", UUID.class), instant(rs.getTimestamp("created_at")),
                instant(rs.getTimestamp("decided_at")));
    }

    private String write(Object value) {
        try {
            return json.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Không ghi được JSON của đề xuất", e);
        }
    }

    private <T> T read(String value, TypeReference<T> type) {
        try {
            return json.readValue(value, type);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("JSON của đề xuất trong DB không hợp lệ", e);
        }
    }

    private static Timestamp timestamp(Instant value) {
        return value == null ? null : Timestamp.from(value);
    }

    private static Instant instant(Timestamp value) {
        return value == null ? null : value.toInstant();
    }
}
