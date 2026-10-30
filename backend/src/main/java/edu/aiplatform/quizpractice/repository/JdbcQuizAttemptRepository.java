package edu.aiplatform.quizpractice.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.aiplatform.quizpractice.model.AttemptSnapshot;
import edu.aiplatform.quizpractice.model.AttemptStatus;
import edu.aiplatform.quizpractice.repository.IQuizAttemptRepository;
import edu.aiplatform.quizpractice.model.QuestionResult;
import edu.aiplatform.quizpractice.model.QuizAttempt;
import edu.aiplatform.quizpractice.model.SubmitMode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** {@code quiz_attempts} bằng SQL viết tay; snapshot, câu trả lời, kết quả lưu {@code jsonb}. */
@Repository
public class JdbcQuizAttemptRepository implements IQuizAttemptRepository {

    private static final String COLUMNS = """
            id, quiz_id, class_id, account_id, attempt_no, snapshot, answers, answer_version, status, submit_mode,
            started_at, deadline_at, last_saved_at, submitted_at, score, max_score, correct_count, result""";
    private static final TypeReference<Map<UUID, List<String>>> ANSWERS = new TypeReference<>() { };
    private static final TypeReference<List<QuestionResult>> RESULTS = new TypeReference<>() { };

    private final JdbcClient jdbc;
    private final ObjectMapper json;

    public JdbcQuizAttemptRepository(JdbcClient jdbc, ObjectMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    @Override
    public Optional<QuizAttempt> find(UUID id) {
        return jdbc.sql("select " + COLUMNS + " from quiz_attempts where id = :id").param("id", id)
                .query(this::map).optional();
    }

    @Override
    public void lockStart(UUID quizId, UUID accountId) {
        jdbc.sql("select pg_advisory_xact_lock(hashtextextended(:key, 0))")
                .param("key", "quiz-attempt:" + quizId + ":" + accountId).query((rs, n) -> 1).list();
    }

    @Override
    public Optional<QuizAttempt> findInProgress(UUID quizId, UUID accountId) {
        return jdbc.sql("select " + COLUMNS + " from quiz_attempts where quiz_id = :quizId and account_id = :accountId"
                        + " and status = 'IN_PROGRESS'")
                .param("quizId", quizId).param("accountId", accountId).query(this::map).optional();
    }

    @Override
    public int countAttempts(UUID quizId, UUID accountId) {
        return jdbc.sql("select count(*) from quiz_attempts where quiz_id = :quizId and account_id = :accountId")
                .param("quizId", quizId).param("accountId", accountId).query(Integer.class).single();
    }

    @Override
    public void insert(QuizAttempt a) {
        jdbc.sql("""
                        insert into quiz_attempts (id, quiz_id, class_id, account_id, attempt_no, snapshot, answers,
                            answer_version, status, started_at, deadline_at)
                        values (:id, :quizId, :classId, :accountId, :attemptNo, cast(:snapshot as jsonb),
                            cast(:answers as jsonb), :answerVersion, :status, :startedAt, :deadlineAt)
                        """)
                .param("id", a.id()).param("quizId", a.quizId()).param("classId", a.classId())
                .param("accountId", a.accountId()).param("attemptNo", a.attemptNo())
                .param("snapshot", write(a.snapshot())).param("answers", write(a.answers()))
                .param("answerVersion", a.answerVersion()).param("status", a.status().name())
                .param("startedAt", timestamp(a.startedAt())).param("deadlineAt", timestamp(a.deadlineAt()))
                .update();
    }

    @Override
    public boolean saveAnswers(QuizAttempt a, long expectedVersion) {
        return jdbc.sql("""
                        update quiz_attempts set answers = cast(:answers as jsonb), answer_version = :answerVersion,
                            last_saved_at = :savedAt
                        where id = :id and status = 'IN_PROGRESS' and answer_version = :expected
                        """)
                .param("answers", write(a.answers())).param("answerVersion", a.answerVersion())
                .param("savedAt", timestamp(a.lastSavedAt())).param("id", a.id()).param("expected", expectedVersion)
                .update() == 1;
    }

    @Override
    public boolean submit(QuizAttempt a) {
        return jdbc.sql("""
                        update quiz_attempts set status = 'SUBMITTED', submit_mode = :mode, submitted_at = :submittedAt,
                            score = :score, max_score = :maxScore, correct_count = :correct,
                            result = cast(:result as jsonb)
                        where id = :id and status = 'IN_PROGRESS' and answer_version = :answerVersion
                        """)
                .param("mode", a.submitMode().name()).param("submittedAt", timestamp(a.submittedAt()))
                .param("score", a.score()).param("maxScore", a.maxScore()).param("correct", a.correctCount())
                .param("result", write(a.result())).param("id", a.id()).param("answerVersion", a.answerVersion())
                .update() == 1;
    }

    @Override
    public List<QuizAttempt> listByAccount(UUID accountId, Collection<UUID> quizIds) {
        if (quizIds.isEmpty()) {
            return List.of();
        }
        return jdbc.sql("select " + COLUMNS + " from quiz_attempts where account_id = :accountId and quiz_id in (:quizIds)"
                        + " order by started_at desc")
                .param("accountId", accountId).param("quizIds", List.copyOf(quizIds)).query(this::map).list();
    }

    @Override
    public Map<UUID, UUID> quizzesAttemptedBy(UUID accountId) {
        Map<UUID, UUID> result = new LinkedHashMap<>();
        jdbc.sql("select distinct on (quiz_id) quiz_id, class_id from quiz_attempts where account_id = :accountId"
                        + " order by quiz_id, started_at desc")
                .param("accountId", accountId)
                .query((rs, n) -> result.put(rs.getObject("quiz_id", UUID.class), rs.getObject("class_id", UUID.class)))
                .list();
        return result;
    }

    @Override
    public List<QuizAttempt> findOverdue(Instant cutoff, int limit) {
        return jdbc.sql("select " + COLUMNS + " from quiz_attempts where status = 'IN_PROGRESS' and deadline_at < :cutoff"
                        + " order by deadline_at limit :limit")
                .param("cutoff", timestamp(cutoff)).param("limit", limit).query(this::map).list();
    }

    @Override
    public List<QuizAttempt> findInProgressByQuiz(UUID quizId) {
        return jdbc.sql("select " + COLUMNS + " from quiz_attempts where quiz_id = :quizId and status = 'IN_PROGRESS'"
                        + " for update")
                .param("quizId", quizId).query(this::map).list();
    }

    @Override
    public List<QuizAttempt> listByQuizAndClass(UUID quizId, UUID classId) {
        return jdbc.sql("select " + COLUMNS + " from quiz_attempts where quiz_id = :quizId and class_id = :classId"
                        + " order by account_id, attempt_no")
                .param("quizId", quizId).param("classId", classId).query(this::map).list();
    }

    private QuizAttempt map(ResultSet rs, int row) throws SQLException {
        String mode = rs.getString("submit_mode");
        String result = rs.getString("result");
        Object correct = rs.getObject("correct_count");
        return new QuizAttempt(rs.getObject("id", UUID.class), rs.getObject("quiz_id", UUID.class),
                rs.getObject("class_id", UUID.class), rs.getObject("account_id", UUID.class), rs.getInt("attempt_no"),
                read(rs.getString("snapshot"), AttemptSnapshot.class), read(rs.getString("answers"), ANSWERS),
                rs.getLong("answer_version"), AttemptStatus.valueOf(rs.getString("status")),
                mode == null ? null : SubmitMode.valueOf(mode), instant(rs.getTimestamp("started_at")),
                instant(rs.getTimestamp("deadline_at")), instant(rs.getTimestamp("last_saved_at")),
                instant(rs.getTimestamp("submitted_at")), rs.getBigDecimal("score"), rs.getBigDecimal("max_score"),
                correct == null ? null : ((Number) correct).intValue(), result == null ? null : read(result, RESULTS));
    }

    private String write(Object value) {
        try {
            return json.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Không ghi được JSON của lượt làm", e);
        }
    }

    private <T> T read(String value, Class<T> type) {
        try {
            return json.readValue(value, type);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("JSON của lượt làm trong DB không hợp lệ", e);
        }
    }

    private <T> T read(String value, TypeReference<T> type) {
        try {
            return json.readValue(value, type);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("JSON của lượt làm trong DB không hợp lệ", e);
        }
    }

    private static Timestamp timestamp(Instant value) {
        return value == null ? null : Timestamp.from(value);
    }

    private static Instant instant(Timestamp value) {
        return value == null ? null : value.toInstant();
    }
}
