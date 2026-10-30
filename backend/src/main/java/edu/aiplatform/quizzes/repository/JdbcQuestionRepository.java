package edu.aiplatform.quizzes.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.aiplatform.quizzes.contract.QuestionType;
import edu.aiplatform.quizzes.model.Difficulty;
import edu.aiplatform.quizzes.repository.IQuestionRepository;
import edu.aiplatform.quizzes.model.Question;
import edu.aiplatform.quizzes.model.QuestionContent;
import edu.aiplatform.quizzes.model.QuestionOption;
import edu.aiplatform.quizzes.model.QuestionScope;
import edu.aiplatform.quizzes.model.QuestionStatus;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** {@code questions} bằng SQL viết tay; lựa chọn, đáp án, tag lưu {@code jsonb} (D1 của plan giai đoạn 1). */
@Repository
public class JdbcQuestionRepository implements IQuestionRepository {

    private static final String COLUMNS = """
            id, lineage_id, version_no, scope, subject_id, quiz_id, type, stem, explanation, options,
            correct_option_ids, default_points, difficulty, tags, status, activated_at, created_by, created_at,
            updated_at, version""";
    private static final TypeReference<List<QuestionOption>> OPTIONS = new TypeReference<>() { };
    private static final TypeReference<List<String>> STRINGS = new TypeReference<>() { };

    private final JdbcClient jdbc;
    private final ObjectMapper json;

    public JdbcQuestionRepository(JdbcClient jdbc, ObjectMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    @Override
    public Optional<Question> find(UUID id) {
        return jdbc.sql("select " + COLUMNS + " from questions where id = :id")
                .param("id", id).query(this::map).optional();
    }

    @Override
    public Optional<Question> findBankDraft(UUID lineageId) {
        return jdbc.sql("select " + COLUMNS + " from questions where lineage_id = :lineageId and scope = 'BANK'"
                        + " and status = 'DRAFT'")
                .param("lineageId", lineageId).query(this::map).optional();
    }

    @Override
    public List<Question> lineage(UUID lineageId) {
        return jdbc.sql("select " + COLUMNS + " from questions where lineage_id = :lineageId order by version_no desc")
                .param("lineageId", lineageId).query(this::map).list();
    }

    @Override
    public int maxVersionNo(UUID lineageId) {
        return jdbc.sql("select coalesce(max(version_no), 0) from questions where lineage_id = :lineageId")
                .param("lineageId", lineageId).query(Integer.class).single();
    }

    @Override
    public void insert(Question q) {
        QuestionContent c = q.content();
        jdbc.sql("""
                        insert into questions (id, lineage_id, version_no, scope, subject_id, quiz_id, type, stem,
                            explanation, options, correct_option_ids, default_points, difficulty, tags, status,
                            activated_at, created_by, created_at, updated_at, version)
                        values (:id, :lineageId, :versionNo, :scope, :subjectId, :quizId, :type, :stem, :explanation,
                            cast(:options as jsonb), cast(:correct as jsonb), :points, :difficulty,
                            cast(:tags as jsonb), :status, :activatedAt, :createdBy, :createdAt, :updatedAt, :version)
                        """)
                .param("id", q.id()).param("lineageId", q.lineageId()).param("versionNo", q.versionNo())
                .param("scope", q.scope().name()).param("subjectId", q.subjectId()).param("quizId", q.quizId())
                .param("type", c.type().name()).param("stem", c.stem()).param("explanation", c.explanation())
                .param("options", write(c.options())).param("correct", write(c.correctOptionIds()))
                .param("points", c.defaultPoints()).param("difficulty", c.difficulty().name())
                .param("tags", write(c.tags())).param("status", q.status().name())
                .param("activatedAt", timestamp(q.activatedAt())).param("createdBy", q.createdBy())
                .param("createdAt", timestamp(q.createdAt())).param("updatedAt", timestamp(q.updatedAt()))
                .param("version", q.version())
                .update();
    }

    @Override
    public boolean update(Question q) {
        QuestionContent c = q.content();
        return jdbc.sql("""
                        update questions set type = :type, stem = :stem, explanation = :explanation,
                            options = cast(:options as jsonb), correct_option_ids = cast(:correct as jsonb),
                            default_points = :points, difficulty = :difficulty, tags = cast(:tags as jsonb),
                            status = :status, activated_at = :activatedAt, updated_at = :updatedAt,
                            version = version + 1
                        where id = :id and version = :version
                        """)
                .param("type", c.type().name()).param("stem", c.stem()).param("explanation", c.explanation())
                .param("options", write(c.options())).param("correct", write(c.correctOptionIds()))
                .param("points", c.defaultPoints()).param("difficulty", c.difficulty().name())
                .param("tags", write(c.tags())).param("status", q.status().name())
                .param("activatedAt", timestamp(q.activatedAt())).param("updatedAt", timestamp(q.updatedAt()))
                .param("id", q.id()).param("version", q.version())
                .update() == 1;
    }

    @Override
    public boolean delete(UUID id, long version) {
        return jdbc.sql("delete from questions where id = :id and version = :version and status = 'DRAFT'"
                        + " and activated_at is null")
                .param("id", id).param("version", version).update() == 1;
    }

    @Override
    public BankPage searchBank(UUID subjectId, BankQuery query) {
        // Mỗi lineage lấy bản đã kích hoạt mới nhất (ACTIVE hoặc RETIRED); lineage chỉ có nháp thì lấy bản nháp.
        StringBuilder where = new StringBuilder("rn = 1");
        List<Object[]> params = new ArrayList<>();
        params.add(new Object[] {"subjectId", subjectId});
        if (query.type() != null) {
            where.append(" and type = :type");
            params.add(new Object[] {"type", query.type().name()});
        }
        if (query.difficulty() != null) {
            where.append(" and difficulty = :difficulty");
            params.add(new Object[] {"difficulty", query.difficulty().name()});
        }
        if (query.status() != null) {
            where.append(" and status = :status");
            params.add(new Object[] {"status", query.status().name()});
        }
        if (query.text() != null && !query.text().isBlank()) {
            where.append(" and (stem ilike :text escape '\\' or exists (select 1 from jsonb_array_elements_text(tags) t"
                    + " where t ilike :text escape '\\'))");
            params.add(new Object[] {"text", "%" + escapeLike(query.text().strip()) + "%"});
        }
        String body = """
                with ranked as (
                    select %s,
                        row_number() over (partition by lineage_id
                            order by (status <> 'DRAFT') desc, version_no desc) as rn,
                        bool_or(status = 'DRAFT') over (partition by lineage_id) as lineage_has_draft
                    from questions
                    where scope = 'BANK' and subject_id = :subjectId
                )
                select %s, (lineage_has_draft and status <> 'DRAFT') as has_draft, count(*) over () as total
                from ranked
                where %s
                order by updated_at desc, id
                limit :limit offset :offset
                """.formatted(COLUMNS, COLUMNS, where);
        JdbcClient.StatementSpec sql = jdbc.sql(body);
        for (Object[] p : params) {
            sql = sql.param((String) p[0], p[1]);
        }
        long[] total = {0};
        List<BankRow> rows = sql.param("limit", query.size()).param("offset", (long) query.page() * query.size())
                .query((rs, n) -> {
                    total[0] = rs.getLong("total");
                    return new BankRow(map(rs, n), rs.getBoolean("has_draft"));
                })
                .list();
        if (rows.isEmpty() && query.page() > 0) {
            total[0] = countBank(subjectId, body.replace("limit :limit offset :offset", ""), params);
        }
        return new BankPage(rows, total[0], query.page(), query.size());
    }

    private long countBank(UUID subjectId, String body, List<Object[]> params) {
        JdbcClient.StatementSpec sql = jdbc.sql("select count(*) from (" + body + ") counted");
        for (Object[] p : params) {
            sql = sql.param((String) p[0], p[1]);
        }
        return sql.query(Long.class).single();
    }

    @Override
    public int countQuizzesUsing(UUID questionId) {
        return jdbc.sql("select count(distinct quiz_id) from quiz_items where question_id = :id")
                .param("id", questionId).query(Integer.class).single();
    }

    @Override
    public List<Question> findAll(Collection<UUID> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        return jdbc.sql("select " + COLUMNS + " from questions where id in (:ids)")
                .param("ids", List.copyOf(ids)).query(this::map).list();
    }

    @Override
    public List<Question> inlineOf(UUID quizId) {
        return jdbc.sql("select " + COLUMNS + " from questions where scope = 'QUIZ' and quiz_id = :quizId")
                .param("quizId", quizId).query(this::map).list();
    }

    @Override
    public boolean deleteInline(UUID questionId, UUID quizId) {
        return jdbc.sql("delete from questions where id = :id and scope = 'QUIZ' and quiz_id = :quizId")
                .param("id", questionId).param("quizId", quizId).update() == 1;
    }

    @Override
    public BankPage searchActive(UUID subjectId, PickQuery query) {
        Filter filter = pickFilter(subjectId, query);
        String sql = activeSql(filter.where) + " order by updated_at desc, id limit :limit offset :offset";
        long[] total = {0};
        List<BankRow> rows = bind(jdbc.sql(sql), filter.params)
                .param("limit", query.size()).param("offset", (long) query.page() * query.size())
                .query((rs, n) -> {
                    total[0] = rs.getLong("total");
                    return new BankRow(map(rs, n), false);
                })
                .list();
        if (rows.isEmpty() && query.page() > 0) {
            total[0] = countActive(subjectId, query);
        }
        return new BankPage(rows, total[0], query.page(), query.size());
    }

    @Override
    public List<Question> randomActive(UUID subjectId, PickQuery query, int count) {
        Filter filter = pickFilter(subjectId, query);
        return bind(jdbc.sql(activeSql(filter.where) + " order by random() limit :count"), filter.params)
                .param("count", count).query(this::map).list();
    }

    @Override
    public long countActive(UUID subjectId, PickQuery query) {
        Filter filter = pickFilter(subjectId, query);
        return bind(jdbc.sql("select count(*) from (" + activeSql(filter.where) + ") counted"), filter.params)
                .query(Long.class).single();
    }

    private record Filter(String where, List<Object[]> params) {
    }

    /** Bản đã kích hoạt mới nhất mỗi lineage còn {@code ACTIVE}; lọc độ khó, thẻ, từ khóa, bỏ lineage đã có. */
    private static Filter pickFilter(UUID subjectId, PickQuery query) {
        StringBuilder where = new StringBuilder("rn = 1 and status = 'ACTIVE'");
        List<Object[]> params = new ArrayList<>();
        params.add(new Object[] {"subjectId", subjectId});
        if (query.difficulty() != null) {
            where.append(" and difficulty = :difficulty");
            params.add(new Object[] {"difficulty", query.difficulty().name()});
        }
        if (query.tag() != null && !query.tag().isBlank()) {
            where.append(" and exists (select 1 from jsonb_array_elements_text(tags) t where lower(t) = lower(:tag))");
            params.add(new Object[] {"tag", query.tag().strip()});
        }
        if (query.text() != null && !query.text().isBlank()) {
            where.append(" and (stem ilike :text escape '\\' or exists (select 1 from jsonb_array_elements_text(tags) t"
                    + " where t ilike :text escape '\\'))");
            params.add(new Object[] {"text", "%" + escapeLike(query.text().strip()) + "%"});
        }
        if (query.excludeLineages() != null && !query.excludeLineages().isEmpty()) {
            where.append(" and lineage_id not in (:excluded)");
            params.add(new Object[] {"excluded", List.copyOf(query.excludeLineages())});
        }
        return new Filter(where.toString(), params);
    }

    private static String activeSql(String where) {
        return """
                with ranked as (
                    select %s,
                        row_number() over (partition by lineage_id
                            order by (status <> 'DRAFT') desc, version_no desc) as rn
                    from questions
                    where scope = 'BANK' and subject_id = :subjectId
                )
                select %s, count(*) over () as total
                from ranked
                where %s""".formatted(COLUMNS, COLUMNS, where);
    }

    private static JdbcClient.StatementSpec bind(JdbcClient.StatementSpec sql, List<Object[]> params) {
        for (Object[] p : params) {
            sql = sql.param((String) p[0], p[1]);
        }
        return sql;
    }

    private Question map(ResultSet rs, int row) throws SQLException {
        QuestionContent content = new QuestionContent(QuestionType.valueOf(rs.getString("type")), rs.getString("stem"),
                read(rs.getString("options"), OPTIONS), read(rs.getString("correct_option_ids"), STRINGS),
                rs.getString("explanation"), rs.getBigDecimal("default_points"),
                Difficulty.valueOf(rs.getString("difficulty")), read(rs.getString("tags"), STRINGS));
        return new Question(rs.getObject("id", UUID.class), rs.getObject("lineage_id", UUID.class),
                rs.getInt("version_no"), QuestionScope.valueOf(rs.getString("scope")),
                rs.getObject("subject_id", UUID.class), rs.getObject("quiz_id", UUID.class), content,
                QuestionStatus.valueOf(rs.getString("status")), instant(rs.getTimestamp("activated_at")),
                rs.getObject("created_by", UUID.class), instant(rs.getTimestamp("created_at")),
                instant(rs.getTimestamp("updated_at")), rs.getLong("version"));
    }

    private String write(Object value) {
        try {
            return json.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Không ghi được JSON của câu hỏi", e);
        }
    }

    private <T> T read(String value, TypeReference<T> type) {
        try {
            return json.readValue(value, type);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("JSON của câu hỏi trong DB không hợp lệ", e);
        }
    }

    private static String escapeLike(String text) {
        return text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private static Timestamp timestamp(Instant value) {
        return value == null ? null : Timestamp.from(value);
    }

    private static Instant instant(Timestamp value) {
        return value == null ? null : value.toInstant();
    }
}
