package edu.aiplatform.quizzes.infrastructure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.aiplatform.quizzes.contract.QuizScope;
import edu.aiplatform.quizzes.contract.QuizSettings;
import edu.aiplatform.quizzes.contract.QuizStatus;
import edu.aiplatform.quizzes.domain.IQuizRepository;
import edu.aiplatform.quizzes.domain.Quiz;
import edu.aiplatform.quizzes.domain.QuizItem;
import edu.aiplatform.quizzes.domain.QuizOrigin;
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

/** {@code quizzes}, {@code quiz_items} bằng SQL viết tay; cài đặt và học liệu gắn kèm lưu {@code jsonb}. */
@Repository
public class JdbcQuizRepository implements IQuizRepository {

    private static final String COLUMNS = """
            id, scope, class_id, subject_id, source_quiz_id, origin, version_no, title, instructions, settings,
            activity_monitoring_enabled, activity_notice, reference_material_ids, status, published_at, retired_at,
            retired_reason, created_by, created_at, updated_at, version""";
    /** Số câu và tổng điểm của Quiz (BR-U10-13). */
    private static final String SUMMARY = COLUMNS + """
            , (select count(*) from quiz_items i where i.quiz_id = quizzes.id) as item_count,
            (select coalesce(sum(i.points), 0) from quiz_items i where i.quiz_id = quizzes.id) as max_score""";
    /** Mọi Quiz cùng chuỗi version với {@code :id}: đi lên tới Quiz gốc rồi đi xuống theo {@code NEW_VERSION}. */
    private static final String VERSION_CHAIN = """
            with recursive up as (
                select id, source_quiz_id, origin from quizzes where id = :id
                union all
                select q.id, q.source_quiz_id, q.origin from quizzes q join up on q.id = up.source_quiz_id
                where up.origin = 'NEW_VERSION'
            ),
            root as (select id from up where origin <> 'NEW_VERSION' or source_quiz_id is null),
            down as (
                select id from root
                union all
                select q.id from quizzes q join down on q.source_quiz_id = down.id where q.origin = 'NEW_VERSION'
            )
            """;
    private static final TypeReference<List<UUID>> UUIDS = new TypeReference<>() { };

    private final JdbcClient jdbc;
    private final ObjectMapper json;

    public JdbcQuizRepository(JdbcClient jdbc, ObjectMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    @Override
    public Optional<Quiz> find(UUID id) {
        return jdbc.sql("select " + COLUMNS + " from quizzes where id = :id").param("id", id).query(this::map).optional();
    }

    @Override
    public void insert(Quiz q) {
        jdbc.sql("""
                        insert into quizzes (id, scope, class_id, subject_id, source_quiz_id, origin, version_no, title,
                            instructions, settings, activity_monitoring_enabled, activity_notice,
                            reference_material_ids, status, published_at, retired_at, retired_reason, created_by,
                            created_at, updated_at, version)
                        values (:id, :scope, :classId, :subjectId, :sourceQuizId, :origin, :versionNo, :title,
                            :instructions, cast(:settings as jsonb), :monitoring, :notice, cast(:materials as jsonb),
                            :status, :publishedAt, :retiredAt, :retiredReason, :createdBy, :createdAt, :updatedAt,
                            :version)
                        """)
                .param("id", q.id()).param("scope", q.scope().name()).param("classId", q.classId())
                .param("subjectId", q.subjectId()).param("sourceQuizId", q.sourceQuizId())
                .param("origin", q.origin().name()).param("versionNo", q.versionNo()).param("title", q.title())
                .param("instructions", q.instructions()).param("settings", write(q.settings()))
                .param("monitoring", q.activityMonitoringEnabled()).param("notice", q.activityNotice())
                .param("materials", write(q.referenceMaterialIds())).param("status", q.status().name())
                .param("publishedAt", timestamp(q.publishedAt())).param("retiredAt", timestamp(q.retiredAt()))
                .param("retiredReason", q.retiredReason()).param("createdBy", q.createdBy())
                .param("createdAt", timestamp(q.createdAt())).param("updatedAt", timestamp(q.updatedAt()))
                .param("version", q.version())
                .update();
    }

    @Override
    public boolean update(Quiz q) {
        return jdbc.sql("""
                        update quizzes set title = :title, instructions = :instructions,
                            settings = cast(:settings as jsonb), activity_monitoring_enabled = :monitoring,
                            activity_notice = :notice, reference_material_ids = cast(:materials as jsonb),
                            status = :status, published_at = :publishedAt, retired_at = :retiredAt,
                            retired_reason = :retiredReason, updated_at = :updatedAt, version = version + 1
                        where id = :id and version = :version
                        """)
                .param("title", q.title()).param("instructions", q.instructions())
                .param("settings", write(q.settings())).param("monitoring", q.activityMonitoringEnabled())
                .param("notice", q.activityNotice()).param("materials", write(q.referenceMaterialIds()))
                .param("status", q.status().name()).param("publishedAt", timestamp(q.publishedAt()))
                .param("retiredAt", timestamp(q.retiredAt())).param("retiredReason", q.retiredReason())
                .param("updatedAt", timestamp(q.updatedAt())).param("id", q.id()).param("version", q.version())
                .update() == 1;
    }

    @Override
    public boolean delete(UUID id, long version) {
        boolean deletable = jdbc.sql("""
                        select 1 from quizzes where id = :id and version = :version and published_at is null
                            and status in ('DRAFT', 'REVIEWED')
                        for update
                        """)
                .param("id", id).param("version", version).query(Integer.class).optional().isPresent();
        if (!deletable) {
            return false;
        }
        jdbc.sql("delete from quiz_items where quiz_id = :id").param("id", id).update();
        jdbc.sql("delete from quiz_ai_drafts where quiz_id = :id").param("id", id).update();
        jdbc.sql("delete from questions where scope = 'QUIZ' and quiz_id = :id").param("id", id).update();
        return jdbc.sql("delete from quizzes where id = :id").param("id", id).update() == 1;
    }

    @Override
    public List<QuizItem> items(UUID quizId) {
        return jdbc.sql("select quiz_id, question_id, position, points from quiz_items where quiz_id = :id order by position")
                .param("id", quizId)
                .query((rs, n) -> new QuizItem(rs.getObject("quiz_id", UUID.class), rs.getObject("question_id", UUID.class),
                        rs.getInt("position"), rs.getBigDecimal("points")))
                .list();
    }

    @Override
    public void replaceItems(UUID quizId, List<QuizItem> items) {
        jdbc.sql("delete from quiz_items where quiz_id = :id").param("id", quizId).update();
        for (QuizItem item : items) {
            jdbc.sql("insert into quiz_items (quiz_id, question_id, position, points) values (:quizId, :questionId,"
                            + " :position, :points)")
                    .param("quizId", quizId).param("questionId", item.questionId()).param("position", item.position())
                    .param("points", item.points()).update();
        }
    }

    @Override
    public QuizPage listSubjectQuizzes(UUID subjectId, QuizStatus status, String text, int page, int size) {
        StringBuilder where = new StringBuilder("scope = 'SUBJECT' and subject_id = :subjectId");
        if (status != null) {
            where.append(" and status = :status");
        }
        boolean search = text != null && !text.isBlank();
        if (search) {
            where.append(" and title ilike :text escape '\\'");
        }
        JdbcClient.StatementSpec sql = jdbc.sql("select " + SUMMARY + ", count(*) over () as total from quizzes where "
                        + where + " order by updated_at desc, id limit :limit offset :offset")
                .param("subjectId", subjectId).param("limit", size).param("offset", (long) page * size);
        if (status != null) {
            sql = sql.param("status", status.name());
        }
        if (search) {
            sql = sql.param("text", "%" + text.strip().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%");
        }
        long[] total = {0};
        List<QuizSummary> items = sql.query((rs, n) -> {
            total[0] = rs.getLong("total");
            return summary(rs, n);
        }).list();
        return new QuizPage(items, total[0], page, size);
    }

    @Override
    public List<QuizSummary> listClassQuizzes(UUID classId) {
        return jdbc.sql("select " + SUMMARY + " from quizzes where scope = 'CLASS' and class_id = :classId"
                        + " order by updated_at desc, id")
                .param("classId", classId).query(this::summary).list();
    }

    @Override
    public List<QuizSummary> listSubjectQuizzes(UUID subjectId, Collection<QuizStatus> statuses) {
        return jdbc.sql("select " + SUMMARY + " from quizzes where scope = 'SUBJECT' and subject_id = :subjectId"
                        + " and status in (:statuses) order by updated_at desc, id")
                .param("subjectId", subjectId).param("statuses", statuses.stream().map(Enum::name).toList())
                .query(this::summary).list();
    }

    @Override
    public List<QuizSummary> listClassQuizzesOf(Collection<UUID> classIds, UUID excludeClassId) {
        if (classIds.isEmpty()) {
            return List.of();
        }
        return jdbc.sql("select " + SUMMARY + " from quizzes where scope = 'CLASS' and class_id in (:classIds)"
                        + " and class_id <> :exclude order by updated_at desc, id")
                .param("classIds", List.copyOf(classIds)).param("exclude", excludeClassId)
                .query(this::summary).list();
    }

    @Override
    public List<QuizSummary> listOpenByMaterial(UUID materialId, UUID classId, UUID subjectId) {
        return jdbc.sql("select " + SUMMARY + " from quizzes where status = 'OPEN'"
                        + " and reference_material_ids @> jsonb_build_array(cast(:materialId as text))"
                        + " and ((scope = 'CLASS' and class_id = :classId) or (scope = 'SUBJECT' and subject_id = :subjectId))"
                        + " order by updated_at desc, id")
                .param("materialId", materialId.toString()).param("classId", classId).param("subjectId", subjectId)
                .query(this::summary).list();
    }

    @Override
    public Optional<Quiz> findOpenInVersionChain(UUID quizId) {
        return jdbc.sql(VERSION_CHAIN + "select " + COLUMNS + " from quizzes where id in (select id from down)"
                        + " and id <> :id and status = 'OPEN' for update")
                .param("id", quizId).query(this::map).optional();
    }

    @Override
    public int maxVersionNo(UUID quizId) {
        return jdbc.sql(VERSION_CHAIN + "select coalesce(max(version_no), 0) from quizzes where id in (select id from down)")
                .param("id", quizId).query(Integer.class).single();
    }

    private QuizSummary summary(ResultSet rs, int row) throws SQLException {
        return new QuizSummary(map(rs, row), rs.getInt("item_count"), rs.getBigDecimal("max_score"));
    }

    private Quiz map(ResultSet rs, int row) throws SQLException {
        return new Quiz(rs.getObject("id", UUID.class), QuizScope.valueOf(rs.getString("scope")),
                rs.getObject("class_id", UUID.class), rs.getObject("subject_id", UUID.class),
                rs.getObject("source_quiz_id", UUID.class), QuizOrigin.valueOf(rs.getString("origin")),
                rs.getInt("version_no"), rs.getString("title"), rs.getString("instructions"),
                read(rs.getString("settings"), QuizSettings.class), rs.getBoolean("activity_monitoring_enabled"),
                rs.getString("activity_notice"), read(rs.getString("reference_material_ids"), UUIDS),
                QuizStatus.valueOf(rs.getString("status")), instant(rs.getTimestamp("published_at")),
                instant(rs.getTimestamp("retired_at")), rs.getString("retired_reason"),
                rs.getObject("created_by", UUID.class), instant(rs.getTimestamp("created_at")),
                instant(rs.getTimestamp("updated_at")), rs.getLong("version"));
    }

    private String write(Object value) {
        try {
            return json.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Không ghi được JSON của quiz", e);
        }
    }

    private <T> T read(String value, Class<T> type) {
        try {
            return json.readValue(value, type);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("JSON của quiz trong DB không hợp lệ", e);
        }
    }

    private <T> T read(String value, TypeReference<T> type) {
        try {
            return json.readValue(value, type);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("JSON của quiz trong DB không hợp lệ", e);
        }
    }

    private static Timestamp timestamp(Instant value) {
        return value == null ? null : Timestamp.from(value);
    }

    private static Instant instant(Timestamp value) {
        return value == null ? null : value.toInstant();
    }
}
