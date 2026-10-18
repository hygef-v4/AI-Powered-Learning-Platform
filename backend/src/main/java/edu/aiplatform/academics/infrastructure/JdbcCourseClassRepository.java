package edu.aiplatform.academics.infrastructure;

import edu.aiplatform.academics.contract.ClassStatus;
import edu.aiplatform.academics.domain.AcademicRuleException;
import edu.aiplatform.academics.domain.CourseClass;
import edu.aiplatform.academics.domain.ICourseClassRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "platform.academics.persistence-enabled", havingValue = "true")
public class JdbcCourseClassRepository implements ICourseClassRepository {
    private final NamedParameterJdbcTemplate jdbc;

    public JdbcCourseClassRepository(NamedParameterJdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override public Optional<CourseClass> find(UUID classId) {
        return jdbc.query("select * from course_classes where id = :id", Map.of("id", classId), JdbcCourseClassRepository::map)
                .stream().findFirst();
    }

    @Override public List<CourseClass> listBySubject(UUID subjectId, int limit, int offset) {
        PersistenceChecks.page(limit, offset);
        return jdbc.query("select * from course_classes where subject_id = :subjectId order by code, id limit :limit offset :offset",
                Map.of("subjectId", subjectId, "limit", limit, "offset", offset), JdbcCourseClassRepository::map);
    }

    @Override public void insert(CourseClass value) {
        PersistenceChecks.writableTransaction();
        if (value.version() != 0) throw new IllegalArgumentException("New class version must be zero");
        try {
            jdbc.update("""
                    insert into course_classes (id, subject_id, code, name, description, term, status, teacher_id,
                        show_grade_distribution, created_at, updated_at, version)
                    values (:id, :subjectId, :code, :name, :description, :term, :status, :teacherId,
                        :distribution, :createdAt, :updatedAt, :version)
                    """, params(value));
        } catch (DuplicateKeyException ex) {
            throw new AcademicRuleException("CLASS_EXISTS", "Mã lớp trong môn hoặc ID lớp đã tồn tại");
        }
    }

    @Override public void update(CourseClass value, long expectedVersion) {
        PersistenceChecks.writableTransaction();
        if (expectedVersion < 0 || expectedVersion == Long.MAX_VALUE || value.version() != expectedVersion + 1) {
            throw new IllegalArgumentException("Updated class must advance expected version by one");
        }
        var args = params(value);
        args.put("expectedVersion", expectedVersion);
        int count = jdbc.update("""
                update course_classes set name = :name, description = :description, term = :term,
                    status = :status, teacher_id = :teacherId, show_grade_distribution = :distribution,
                    updated_at = :updatedAt, version = :version
                where id = :id and version = :expectedVersion
                """, args);
        if (count != 1) throw new AcademicRuleException("CLASS_VERSION_CONFLICT", "Dữ liệu lớp đã thay đổi. Hãy tải lại");
    }

    private static Map<String, Object> params(CourseClass value) {
        var args = new HashMap<String, Object>();
        args.put("id", value.id()); args.put("subjectId", value.subjectId()); args.put("code", value.code());
        args.put("name", value.name()); args.put("description", value.description()); args.put("term", value.term());
        args.put("status", value.status().name()); args.put("teacherId", value.teacherId());
        args.put("distribution", value.showGradeDistribution()); args.put("version", value.version());
        args.put("createdAt", Timestamp.from(value.createdAt())); args.put("updatedAt", Timestamp.from(value.updatedAt()));
        return args;
    }

    private static CourseClass map(ResultSet rs, int row) throws SQLException {
        return new CourseClass(rs.getObject("id", UUID.class), rs.getObject("subject_id", UUID.class),
                rs.getString("code"), rs.getString("name"), rs.getString("description"), rs.getString("term"),
                ClassStatus.valueOf(rs.getString("status")), rs.getObject("teacher_id", UUID.class),
                rs.getBoolean("show_grade_distribution"), rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant(), rs.getLong("version"));
    }
}
